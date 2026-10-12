import React, { useState, useEffect } from 'react';
import {
  Container,
  Title,
  Text,
  Group,
  Button,
  Card,
  Table,
  Badge,
  ActionIcon,
  Modal,
  Drawer,
  TextInput,
  Select,
  NumberInput,
  Stack,
  SimpleGrid,
  Divider,
  SegmentedControl,
  Box,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useLocation } from 'react-router-dom';
import {
  IconPlus,
  IconPrinter,
  IconRotateClockwise2,
  IconCheck,
  IconTrash,
  IconSearch,
  IconBuildingStore,
} from '@tabler/icons-react';
import { textileApi, SalesInvoice, Customer, Product, Unit, GstState, InvoiceLine } from '../../api/textileApi';
import { formatCurrency, formatDate } from '../../utils/formatters';

export const SalesInvoicesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const location = useLocation();

  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const [drawerOpened, { open: openDrawer, close: closeDrawer }] = useDisclosure(false);
  const [printModalOpened, { open: openPrintModal, close: closePrintModal }] = useDisclosure(false);
  const [reverseModalOpened, { open: openReverseModal, close: closeReverseModal }] = useDisclosure(false);

  const [selectedInvoice, setSelectedInvoice] = useState<SalesInvoice | null>(null);
  const [reversalReason, setReversalReason] = useState<string>('');

  // Form State
  const [customerId, setCustomerId] = useState<string>('');
  const [invoiceDate, setInvoiceDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [placeOfSupplyCode, setPlaceOfSupplyCode] = useState<string>('33');
  const [notes, setNotes] = useState<string>('');
  const [lines, setLines] = useState<
    {
      productId: string;
      hsnCode: string;
      quantity: number;
      unitId: string;
      rate: number;
      discountAmount: number;
      taxRatePercent: number;
    }[]
  >([
    {
      productId: '',
      hsnCode: '5208',
      quantity: 100,
      unitId: '',
      rate: 150,
      discountAmount: 0,
      taxRatePercent: 5,
    },
  ]);

  // Calculations preview
  const [calcPreview, setCalcPreview] = useState<{
    subtotal: number;
    taxableTotal: number;
    cgstTotal: number;
    sgstTotal: number;
    igstTotal: number;
    roundOff: number;
    grandTotal: number;
    isInterstate: boolean;
  }>({
    subtotal: 0,
    taxableTotal: 0,
    cgstTotal: 0,
    sgstTotal: 0,
    igstTotal: 0,
    roundOff: 0,
    grandTotal: 0,
    isInterstate: false,
  });

  // Queries
  const { data: invoicesData, isLoading } = useQuery({
    queryKey: ['salesInvoices', statusFilter, searchQuery],
    queryFn: () =>
      textileApi.getSalesInvoices({
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        q: searchQuery || undefined,
        size: 50,
      }),
  });

  const { data: customers } = useQuery({
    queryKey: ['customersList'],
    queryFn: () => textileApi.getCustomers({ size: 100 }),
  });

  const { data: products } = useQuery({
    queryKey: ['productsList'],
    queryFn: () => textileApi.getProducts({ size: 100 }),
  });

  const { data: units } = useQuery({
    queryKey: ['unitsList'],
    queryFn: () => textileApi.getUnits(true),
  });

  const { data: gstStates } = useQuery({
    queryKey: ['gstStates'],
    queryFn: textileApi.getGstStates,
  });

  const { data: profile } = useQuery({
    queryKey: ['businessProfile'],
    queryFn: textileApi.getProfile,
  });

  // Auto trigger drawer if URL has ?create=true
  useEffect(() => {
    if (location.search.includes('create=true')) {
      openDrawer();
    }
  }, [location.search, openDrawer]);

  // When customer changes, update place of supply
  useEffect(() => {
    if (customerId && customers?.content) {
      const selected = customers.content.find((c: Customer) => String(c.id) === customerId);
      if (selected?.stateCode) {
        setPlaceOfSupplyCode(selected.stateCode);
      }
    }
  }, [customerId, customers]);

  // Recalculate preview whenever lines or customer state changes
  useEffect(() => {
    if (!customerId || lines.length === 0 || !lines[0].productId || !lines[0].unitId) {
      return;
    }
    const requestData = {
      customerId: Number(customerId),
      invoiceDate,
      placeOfSupplyStateCode: placeOfSupplyCode,
      lines: lines.map((l) => ({
        productId: Number(l.productId),
        hsnCode: l.hsnCode,
        quantity: l.quantity,
        unitId: Number(l.unitId),
        rate: l.rate,
        discountAmount: l.discountAmount,
        taxRatePercent: l.taxRatePercent,
      })),
    };
    textileApi
      .calculateSalesInvoice(requestData)
      .then((res) => {
        setCalcPreview({
          subtotal: Number(res.subtotal),
          taxableTotal: Number(res.taxableTotal),
          cgstTotal: Number(res.cgstTotal),
          sgstTotal: Number(res.sgstTotal),
          igstTotal: Number(res.igstTotal),
          roundOff: Number(res.roundOff),
          grandTotal: Number(res.grandTotal),
          isInterstate: res.isInterstate,
        });
      })
      .catch(() => {
        // Calculation error or incomplete fields
      });
  }, [customerId, invoiceDate, placeOfSupplyCode, lines]);

  // Mutations
  const createDraftMutation = useMutation({
    mutationFn: (andPost: boolean) => {
      const req = {
        customerId: Number(customerId),
        invoiceDate,
        placeOfSupplyStateCode: placeOfSupplyCode,
        notes,
        lines: lines.map((l) => ({
          productId: Number(l.productId),
          hsnCode: l.hsnCode,
          quantity: l.quantity,
          unitId: Number(l.unitId),
          rate: l.rate,
          discountAmount: l.discountAmount,
          taxRatePercent: l.taxRatePercent,
        })),
      };
      return textileApi.createSalesInvoice(req).then((created) => {
        if (andPost) {
          return textileApi.postSalesInvoice(created.id);
        }
        return created;
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['salesInvoices'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      closeDrawer();
    },
  });

  const postMutation = useMutation({
    mutationFn: (id: number) => textileApi.postSalesInvoice(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['salesInvoices'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });

  const reverseMutation = useMutation({
    mutationFn: () => textileApi.reverseSalesInvoice(selectedInvoice!.id, reversalReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['salesInvoices'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      closeReverseModal();
    },
  });

  const handleProductSelect = (index: number, pId: string) => {
    const prod = products?.content.find((p: Product) => String(p.id) === pId);
    setLines((prev) => {
      const copy = [...prev];
      copy[index].productId = pId;
      if (prod) {
        copy[index].hsnCode = prod.hsnCode || '5208';
        copy[index].unitId = String(prod.unitId);
        copy[index].rate = prod.defaultSaleRate || 0;
        copy[index].taxRatePercent = prod.defaultTaxRatePercent || 5;
      }
      return copy;
    });
  };

  const handleAddLine = () => {
    setLines((prev) => [
      ...prev,
      {
        productId: '',
        hsnCode: '5208',
        quantity: 100,
        unitId: units?.[0] ? String(units[0].id) : '',
        rate: 100,
        discountAmount: 0,
        taxRatePercent: 5,
      },
    ]);
  };

  const handleRemoveLine = (index: number) => {
    if (lines.length > 1) {
      setLines((prev) => prev.filter((_, i) => i !== index));
    }
  };

  return (
    <Container size="xl" py="md">
      <Stack gap="lg">
        {/* Top Header */}
        <Group justify="space-between" align="center">
          <div>
            <Title order={2} fw={800} c="dark.8">
              Sales Invoices
            </Title>
            <Text size="sm" c="dimmed">
              GST-compliant tax invoices with automatic CGST, SGST, IGST double-entry postings
            </Text>
          </div>
          <Button
            color="indigo"
            size="sm"
            leftSection={<IconPlus size={16} />}
            onClick={() => {
              if (units && units.length > 0 && !lines[0].unitId) {
                setLines([{ ...lines[0], unitId: String(units[0].id) }]);
              }
              openDrawer();
            }}
          >
            New Tax Invoice
          </Button>
        </Group>

        {/* Filters and search */}
        <Card shadow="xs" padding="sm" radius="md" withBorder bg="white">
          <Group justify="space-between" wrap="wrap">
            <SegmentedControl
              value={statusFilter}
              onChange={setStatusFilter}
              data={[
                { label: 'All Invoices', value: 'ALL' },
                { label: 'Posted', value: 'POSTED' },
                { label: 'Draft', value: 'DRAFT' },
                { label: 'Reversed', value: 'REVERSED' },
              ]}
              size="xs"
              color="indigo"
            />
            <TextInput
              placeholder="Search by invoice # or customer..."
              size="xs"
              leftSection={<IconSearch size={14} />}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.currentTarget.value)}
              style={{ width: 280 }}
            />
          </Group>
        </Card>

        {/* Invoices Table */}
        <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
          <Table.ScrollContainer minWidth={800}>
            <Table striped highlightOnHover verticalSpacing="sm" fz="sm">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Invoice #</Table.Th>
                  <Table.Th>Date</Table.Th>
                  <Table.Th>Customer</Table.Th>
                  <Table.Th>State</Table.Th>
                  <Table.Th ta="right">Taxable</Table.Th>
                  <Table.Th ta="right">GST</Table.Th>
                  <Table.Th ta="right">Grand Total</Table.Th>
                  <Table.Th>Status</Table.Th>
                  <Table.Th ta="center">Actions</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {isLoading ? (
                  <Table.Tr>
                    <Table.Td colSpan={9} ta="center" py="xl">
                      Loading invoices...
                    </Table.Td>
                  </Table.Tr>
                ) : !invoicesData?.content || invoicesData.content.length === 0 ? (
                  <Table.Tr>
                    <Table.Td colSpan={9} ta="center" py="xl" c="dimmed">
                      No sales invoices found. Click &quot;New Tax Invoice&quot; to create one.
                    </Table.Td>
                  </Table.Tr>
                ) : (
                  invoicesData.content.map((inv: SalesInvoice) => {
                    const gstTotal =
                      Number(inv.cgstTotal || 0) + Number(inv.sgstTotal || 0) + Number(inv.igstTotal || 0);
                    return (
                      <Table.Tr key={inv.id}>
                        <Table.Td fw={700} c="indigo.8">
                          {inv.invoiceNo || 'DRAFT'}
                        </Table.Td>
                        <Table.Td>{formatDate(inv.invoiceDate)}</Table.Td>
                        <Table.Td fw={600}>{inv.customerName}</Table.Td>
                        <Table.Td>
                          <Badge size="xs" variant="outline" color={inv.isInterstate ? 'orange' : 'blue'}>
                            {inv.placeOfSupplyStateName} ({inv.placeOfSupplyStateCode})
                          </Badge>
                        </Table.Td>
                        <Table.Td ta="right">{formatCurrency(inv.taxableTotal)}</Table.Td>
                        <Table.Td ta="right">{formatCurrency(gstTotal)}</Table.Td>
                        <Table.Td ta="right" fw={800} c="dark.8">
                          {formatCurrency(inv.grandTotal)}
                        </Table.Td>
                        <Table.Td>
                          <Badge
                            size="sm"
                            color={
                              inv.status === 'POSTED'
                                ? 'teal'
                                : inv.status === 'REVERSED'
                                  ? 'red'
                                  : 'yellow'
                            }
                          >
                            {inv.status}
                          </Badge>
                        </Table.Td>
                        <Table.Td ta="center">
                          <Group gap={6} justify="center">
                            {inv.status === 'POSTED' && (
                              <>
                                <ActionIcon
                                  size="sm"
                                  variant="light"
                                  color="indigo"
                                  title="Print Tax Invoice"
                                  onClick={() => {
                                    setSelectedInvoice(inv);
                                    openPrintModal();
                                  }}
                                >
                                  <IconPrinter size={15} />
                                </ActionIcon>
                                <ActionIcon
                                  size="sm"
                                  variant="light"
                                  color="red"
                                  title="Reverse / Cancel Invoice"
                                  onClick={() => {
                                    setSelectedInvoice(inv);
                                    setReversalReason('');
                                    openReverseModal();
                                  }}
                                >
                                  <IconRotateClockwise2 size={15} />
                                </ActionIcon>
                              </>
                            )}

                            {inv.status === 'DRAFT' && (
                              <Button
                                size="xs"
                                variant="light"
                                color="teal"
                                leftSection={<IconCheck size={12} />}
                                onClick={() => postMutation.mutate(inv.id)}
                                loading={postMutation.isPending}
                              >
                                Post
                              </Button>
                            )}
                          </Group>
                        </Table.Td>
                      </Table.Tr>
                    );
                  })
                )}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </Card>
      </Stack>

      {/* Create Invoice Drawer */}
      <Drawer
        opened={drawerOpened}
        onClose={closeDrawer}
        title={
          <Text fw={700} size="md">
            New GST Sales Tax Invoice
          </Text>
        }
        position="right"
        size="70%"
      >
        <Stack gap="md">
          <SimpleGrid cols={{ base: 1, sm: 3 }}>
            <Select
              label="Customer"
              placeholder="Select Customer"
              required
              data={
                customers?.content.map((c: Customer) => ({
                  value: String(c.id),
                  label: `${c.name} (${c.code}) - ${c.city || c.stateName}`,
                })) || []
              }
              value={customerId}
              onChange={(val) => setCustomerId(val || '')}
              searchable
            />
            <TextInput
              label="Invoice Date"
              type="date"
              required
              value={invoiceDate}
              onChange={(e) => setInvoiceDate(e.currentTarget.value)}
            />
            <Select
              label="Place of Supply (State)"
              placeholder="Select State"
              required
              data={
                gstStates?.map((s: GstState) => ({
                  value: s.code,
                  label: `${s.code} - ${s.name}`,
                })) || []
              }
              value={placeOfSupplyCode}
              onChange={(val) => setPlaceOfSupplyCode(val || '33')}
              searchable
            />
          </SimpleGrid>

          <Divider label="Item Details" labelPosition="center" />

          {/* Line items table */}
          <Table withTableBorder withColumnBorders fz="xs">
            <Table.Thead>
              <Table.Tr bg="gray.1">
                <Table.Th style={{ width: '30%' }}>Product / Item</Table.Th>
                <Table.Th style={{ width: '12%' }}>HSN</Table.Th>
                <Table.Th style={{ width: '12%' }}>Qty</Table.Th>
                <Table.Th style={{ width: '12%' }}>Unit</Table.Th>
                <Table.Th style={{ width: '12%' }}>Rate (₹)</Table.Th>
                <Table.Th style={{ width: '10%' }}>GST %</Table.Th>
                <Table.Th style={{ width: '12%' }}>Line Total</Table.Th>
                <Table.Th style={{ width: '40px' }}></Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {lines.map((line, idx) => {
                const sub = (line.quantity || 0) * (line.rate || 0);
                const tax = (sub * (line.taxRatePercent || 0)) / 100;
                const tot = sub + tax;
                return (
                  <Table.Tr key={idx}>
                    <Table.Td>
                      <Select
                        placeholder="Select Product"
                        size="xs"
                        data={
                          products?.content.map((p: Product) => ({
                            value: String(p.id),
                            label: `${p.name} (${p.code})`,
                          })) || []
                        }
                        value={line.productId}
                        onChange={(val) => handleProductSelect(idx, val || '')}
                        searchable
                      />
                    </Table.Td>
                    <Table.Td>
                      <TextInput
                        size="xs"
                        value={line.hsnCode}
                        onChange={(e) => {
                          const val = e.currentTarget.value;
                          setLines((prev) => {
                            const c = [...prev];
                            c[idx].hsnCode = val;
                            return c;
                          });
                        }}
                      />
                    </Table.Td>
                    <Table.Td>
                      <NumberInput
                        size="xs"
                        min={0.01}
                        value={line.quantity}
                        onChange={(val) => {
                          setLines((prev) => {
                            const c = [...prev];
                            c[idx].quantity = Number(val) || 0;
                            return c;
                          });
                        }}
                      />
                    </Table.Td>
                    <Table.Td>
                      <Select
                        size="xs"
                        data={
                          units?.map((u: Unit) => ({
                            value: String(u.id),
                            label: u.name,
                          })) || []
                        }
                        value={line.unitId}
                        onChange={(val) => {
                          setLines((prev) => {
                            const c = [...prev];
                            c[idx].unitId = val || '';
                            return c;
                          });
                        }}
                      />
                    </Table.Td>
                    <Table.Td>
                      <NumberInput
                        size="xs"
                        min={0}
                        value={line.rate}
                        onChange={(val) => {
                          setLines((prev) => {
                            const c = [...prev];
                            c[idx].rate = Number(val) || 0;
                            return c;
                          });
                        }}
                      />
                    </Table.Td>
                    <Table.Td>
                      <Select
                        size="xs"
                        data={[
                          { value: '0', label: '0%' },
                          { value: '5', label: '5%' },
                          { value: '12', label: '12%' },
                          { value: '18', label: '18%' },
                          { value: '28', label: '28%' },
                        ]}
                        value={String(line.taxRatePercent)}
                        onChange={(val) => {
                          setLines((prev) => {
                            const c = [...prev];
                            c[idx].taxRatePercent = Number(val) || 0;
                            return c;
                          });
                        }}
                      />
                    </Table.Td>
                    <Table.Td ta="right" fw={600}>
                      {formatCurrency(tot)}
                    </Table.Td>
                    <Table.Td>
                      <ActionIcon
                        size="xs"
                        color="red"
                        variant="subtle"
                        onClick={() => handleRemoveLine(idx)}
                        disabled={lines.length === 1}
                      >
                        <IconTrash size={14} />
                      </ActionIcon>
                    </Table.Td>
                  </Table.Tr>
                );
              })}
            </Table.Tbody>
          </Table>

          <Button variant="light" color="indigo" size="xs" onClick={handleAddLine} style={{ alignSelf: 'flex-start' }}>
            + Add Another Item
          </Button>

          {/* Notes and Totals */}
          <SimpleGrid cols={{ base: 1, sm: 2 }} mt="md">
            <TextInput
              label="Invoice Notes / Terms"
              placeholder="e.g. Delivery via VRL Logistics, payment due within 30 days"
              value={notes}
              onChange={(e) => setNotes(e.currentTarget.value)}
            />

            <Card withBorder padding="sm" radius="md" bg="gray.0">
              <Stack gap={4} fz="xs">
                <Group justify="space-between">
                  <Text c="dimmed">Subtotal (Taxable):</Text>
                  <Text fw={600}>{formatCurrency(calcPreview.taxableTotal)}</Text>
                </Group>
                {calcPreview.isInterstate ? (
                  <Group justify="space-between">
                    <Text c="dimmed">IGST Total:</Text>
                    <Text fw={600}>{formatCurrency(calcPreview.igstTotal)}</Text>
                  </Group>
                ) : (
                  <>
                    <Group justify="space-between">
                      <Text c="dimmed">CGST Total:</Text>
                      <Text fw={600}>{formatCurrency(calcPreview.cgstTotal)}</Text>
                    </Group>
                    <Group justify="space-between">
                      <Text c="dimmed">SGST Total:</Text>
                      <Text fw={600}>{formatCurrency(calcPreview.sgstTotal)}</Text>
                    </Group>
                  </>
                )}
                <Group justify="space-between">
                  <Text c="dimmed">Round Off:</Text>
                  <Text fw={600}>{formatCurrency(calcPreview.roundOff)}</Text>
                </Group>
                <Divider my={4} />
                <Group justify="space-between">
                  <Text fw={800} size="sm">
                    Grand Total:
                  </Text>
                  <Text fw={800} size="md" c="indigo.8">
                    {formatCurrency(calcPreview.grandTotal)}
                  </Text>
                </Group>
              </Stack>
            </Card>
          </SimpleGrid>

          <Group justify="flex-end" mt="xl">
            <Button variant="default" onClick={closeDrawer}>
              Cancel
            </Button>
            <Button
              variant="outline"
              color="indigo"
              loading={createDraftMutation.isPending}
              onClick={() => createDraftMutation.mutate(false)}
            >
              Save as Draft
            </Button>
            <Button
              color="indigo"
              loading={createDraftMutation.isPending}
              onClick={() => createDraftMutation.mutate(true)}
            >
              Post &amp; Finalize Invoice
            </Button>
          </Group>
        </Stack>
      </Drawer>

      {/* Reverse Modal */}
      <Modal opened={reverseModalOpened} onClose={closeReverseModal} title="Confirm Invoice Cancellation">
        <Stack gap="md">
          <Text size="sm">
            Are you sure you want to reverse invoice <b>{selectedInvoice?.invoiceNo}</b>?
          </Text>
          <Text size="xs" c="dimmed">
            An exact mirror reversal Journal Entry will be posted to restore customer balances and GST output
            accounts.
          </Text>
          <TextInput
            label="Reversal Reason"
            placeholder="e.g., Goods returned or billed in error"
            required
            value={reversalReason}
            onChange={(e) => setReversalReason(e.currentTarget.value)}
          />
          <Group justify="flex-end">
            <Button variant="default" onClick={closeReverseModal}>
              Dismiss
            </Button>
            <Button color="red" onClick={() => reverseMutation.mutate()} loading={reverseMutation.isPending}>
              Confirm Reversal
            </Button>
          </Group>
        </Stack>
      </Modal>

      {/* Print Tax Invoice Modal */}
      <Modal opened={printModalOpened} onClose={closePrintModal} size="xl" title="Tax Invoice Preview">
        {selectedInvoice && (
          <Box p="md" id="print-tax-invoice" style={{ border: '1px solid #cbd5e1', borderRadius: 8 }}>
            <Group justify="space-between" align="flex-start" mb="md">
              <Group gap="xs">
                <IconBuildingStore size={28} color="#4338ca" />
                <div>
                  <Title order={3} fw={800} c="indigo.8">
                    {profile?.tradeName || 'TEX_ARUN TEXTILES'}
                  </Title>
                  <Text size="xs" c="dimmed">
                    {profile?.legalName}
                  </Text>
                  <Text size="xs">{profile?.address}</Text>
                  <Text size="xs" fw={700}>
                    GSTIN: {profile?.gstin || '33AAAAA0000A1Z5'}
                  </Text>
                </div>
              </Group>
              <div style={{ textAlign: 'right' }}>
                <Badge size="lg" color="indigo" variant="filled">
                  TAX INVOICE
                </Badge>
                <Text size="sm" fw={800} mt={4}>
                  {selectedInvoice.invoiceNo}
                </Text>
                <Text size="xs">Date: {formatDate(selectedInvoice.invoiceDate)}</Text>
                <Text size="xs">Place of Supply: {selectedInvoice.placeOfSupplyStateName}</Text>
              </div>
            </Group>

            <Divider my="sm" />

            <SimpleGrid cols={2} mb="md">
              <Box p="xs" bg="gray.0" style={{ borderRadius: 6 }}>
                <Text fw={700} size="xs" c="dimmed">
                  BILLED TO:
                </Text>
                <Text fw={700} size="sm">
                  {selectedInvoice.customerName}
                </Text>
                <Text size="xs">{selectedInvoice.customerAddress || '-'}</Text>
                <Text size="xs" fw={600}>
                  GSTIN: {selectedInvoice.customerGstin || 'URP (Unregistered)'}
                </Text>
              </Box>
              <Box p="xs" bg="gray.0" style={{ borderRadius: 6 }}>
                <Text fw={700} size="xs" c="dimmed">
                  PAYMENT &amp; BANK DETAILS:
                </Text>
                <Text size="xs">{profile?.bankDetailsText || 'SBI Current A/c: 38920192839, IFSC: SBIN0001234'}</Text>
                <Text size="xs" mt={4} c="dimmed">
                  Notes: {selectedInvoice.notes || 'None'}
                </Text>
              </Box>
            </SimpleGrid>

            <Table withTableBorder withColumnBorders fz="xs" mb="md">
              <Table.Thead bg="gray.1">
                <Table.Tr>
                  <Table.Th>#</Table.Th>
                  <Table.Th>Description</Table.Th>
                  <Table.Th>HSN</Table.Th>
                  <Table.Th ta="right">Qty</Table.Th>
                  <Table.Th>Unit</Table.Th>
                  <Table.Th ta="right">Rate (₹)</Table.Th>
                  <Table.Th ta="right">Taxable</Table.Th>
                  <Table.Th ta="right">GST %</Table.Th>
                  <Table.Th ta="right">Total (₹)</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {selectedInvoice.lines.map((l: InvoiceLine, i: number) => (
                  <Table.Tr key={i}>
                    <Table.Td>{l.lineNo || i + 1}</Table.Td>
                    <Table.Td fw={600}>{l.productName || l.description}</Table.Td>
                    <Table.Td>{l.hsnCode}</Table.Td>
                    <Table.Td ta="right">{l.quantity}</Table.Td>
                    <Table.Td>{l.unitCode}</Table.Td>
                    <Table.Td ta="right">{formatCurrency(l.rate)}</Table.Td>
                    <Table.Td ta="right">{formatCurrency(l.taxableValue)}</Table.Td>
                    <Table.Td ta="right">{l.taxRatePercent}%</Table.Td>
                    <Table.Td ta="right" fw={700}>
                      {formatCurrency(l.lineTotal)}
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>

            <Group justify="flex-end" mb="md">
              <Box style={{ width: 260, borderRadius: 6 }} p="xs" bg="gray.0">
                <Stack gap={2} fz="xs">
                  <Group justify="space-between">
                    <Text>Taxable Amount:</Text>
                    <Text fw={600}>{formatCurrency(selectedInvoice.taxableTotal)}</Text>
                  </Group>
                  {selectedInvoice.isInterstate ? (
                    <Group justify="space-between">
                      <Text>IGST Total:</Text>
                      <Text fw={600}>{formatCurrency(selectedInvoice.igstTotal)}</Text>
                    </Group>
                  ) : (
                    <>
                      <Group justify="space-between">
                        <Text>CGST Total:</Text>
                        <Text fw={600}>{formatCurrency(selectedInvoice.cgstTotal)}</Text>
                      </Group>
                      <Group justify="space-between">
                        <Text>SGST Total:</Text>
                        <Text fw={600}>{formatCurrency(selectedInvoice.sgstTotal)}</Text>
                      </Group>
                    </>
                  )}
                  <Group justify="space-between">
                    <Text>Round Off:</Text>
                    <Text fw={600}>{formatCurrency(selectedInvoice.roundOff)}</Text>
                  </Group>
                  <Divider my={4} />
                  <Group justify="space-between">
                    <Text fw={800} size="sm">
                      Invoice Total:
                    </Text>
                    <Text fw={800} size="sm" c="indigo.8">
                      {formatCurrency(selectedInvoice.grandTotal)}
                    </Text>
                  </Group>
                </Stack>
              </Box>
            </Group>

            <Group justify="space-between" mt="xl" pt="md" style={{ borderTop: '1px dashed #cbd5e1' }}>
              <div>
                <Text size="xs" c="dimmed">
                  {profile?.invoiceFooter || 'Thank you for your business!'}
                </Text>
              </div>
              <div style={{ textAlign: 'right' }}>
                <Text size="xs" fw={700}>
                  For {profile?.tradeName || 'TEX_ARUN'}
                </Text>
                <Text size="xs" c="dimmed" mt={30}>
                  Authorized Signatory
                </Text>
              </div>
            </Group>

            <Group justify="flex-end" mt="lg">
              <Button leftSection={<IconPrinter size={16} />} onClick={() => window.print()} color="indigo">
                Print Invoice
              </Button>
            </Group>
          </Box>
        )}
      </Modal>
    </Container>
  );
};
