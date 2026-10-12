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
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useLocation } from 'react-router-dom';
import {
  IconPlus,
  IconRotateClockwise2,
  IconCheck,
  IconTrash,
  IconSearch,
} from '@tabler/icons-react';
import { textileApi, PurchaseInvoice, Supplier, Product, Unit } from '../../api/textileApi';
import { formatCurrency, formatDate } from '../../utils/formatters';

export const PurchaseInvoicesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const location = useLocation();

  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const [drawerOpened, { open: openDrawer, close: closeDrawer }] = useDisclosure(false);
  const [reverseModalOpened, { open: openReverseModal, close: closeReverseModal }] = useDisclosure(false);

  const [selectedInvoice, setSelectedInvoice] = useState<PurchaseInvoice | null>(null);
  const [reversalReason, setReversalReason] = useState<string>('');

  // Form State
  const [supplierId, setSupplierId] = useState<string>('');
  const [supplierInvoiceNo, setSupplierInvoiceNo] = useState<string>('');
  const [supplierInvoiceDate, setSupplierInvoiceDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [purchaseDate, setPurchaseDate] = useState<string>(new Date().toISOString().split('T')[0]);
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
      hsnCode: '5205',
      quantity: 500,
      unitId: '',
      rate: 220,
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
    queryKey: ['purchaseInvoices', statusFilter, searchQuery],
    queryFn: () =>
      textileApi.getPurchaseInvoices({
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        q: searchQuery || undefined,
        size: 50,
      }),
  });

  const { data: suppliers } = useQuery({
    queryKey: ['suppliersList'],
    queryFn: () => textileApi.getSuppliers({ size: 100 }),
  });

  const { data: products } = useQuery({
    queryKey: ['productsList'],
    queryFn: () => textileApi.getProducts({ size: 100 }),
  });

  const { data: units } = useQuery({
    queryKey: ['unitsList'],
    queryFn: () => textileApi.getUnits(true),
  });

  useEffect(() => {
    if (location.search.includes('create=true')) {
      openDrawer();
    }
  }, [location.search, openDrawer]);

  useEffect(() => {
    if (!supplierId || !supplierInvoiceNo || lines.length === 0 || !lines[0].productId || !lines[0].unitId) {
      return;
    }
    const requestData = {
      supplierId: Number(supplierId),
      supplierInvoiceNo,
      supplierInvoiceDate,
      purchaseDate,
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
      .calculatePurchaseInvoice(requestData)
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
        // Calculation preview incomplete
      });
  }, [supplierId, supplierInvoiceNo, supplierInvoiceDate, purchaseDate, lines]);

  const createDraftMutation = useMutation({
    mutationFn: (andPost: boolean) => {
      const req = {
        supplierId: Number(supplierId),
        supplierInvoiceNo,
        supplierInvoiceDate,
        purchaseDate,
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
      return textileApi.createPurchaseInvoice(req).then((created) => {
        if (andPost) {
          return textileApi.postPurchaseInvoice(created.id);
        }
        return created;
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['purchaseInvoices'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      closeDrawer();
    },
  });

  const postMutation = useMutation({
    mutationFn: (id: number) => textileApi.postPurchaseInvoice(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['purchaseInvoices'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });

  const reverseMutation = useMutation({
    mutationFn: () => textileApi.reversePurchaseInvoice(selectedInvoice!.id, reversalReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['purchaseInvoices'] });
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
        copy[index].hsnCode = prod.hsnCode || '5205';
        copy[index].unitId = String(prod.unitId);
        copy[index].rate = prod.defaultPurchaseRate || 0;
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
        hsnCode: '5205',
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
        {/* Header */}
        <Group justify="space-between" align="center">
          <div>
            <Title order={2} fw={800} c="dark.8">
              Purchase Bills
            </Title>
            <Text size="sm" c="dimmed">
              Yarn and fabric purchase invoices, inward challans, and supplier AP ledger tracking
            </Text>
          </div>
          <Button
            color="teal"
            size="sm"
            leftSection={<IconPlus size={16} />}
            onClick={() => {
              if (units && units.length > 0 && !lines[0].unitId) {
                setLines([{ ...lines[0], unitId: String(units[0].id) }]);
              }
              openDrawer();
            }}
          >
            New Purchase Bill
          </Button>
        </Group>

        {/* Filters */}
        <Card shadow="xs" padding="sm" radius="md" withBorder bg="white">
          <Group justify="space-between" wrap="wrap">
            <SegmentedControl
              value={statusFilter}
              onChange={setStatusFilter}
              data={[
                { label: 'All Bills', value: 'ALL' },
                { label: 'Posted', value: 'POSTED' },
                { label: 'Draft', value: 'DRAFT' },
                { label: 'Reversed', value: 'REVERSED' },
              ]}
              size="xs"
              color="teal"
            />
            <TextInput
              placeholder="Search by voucher #, supplier or invoice #..."
              size="xs"
              leftSection={<IconSearch size={14} />}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.currentTarget.value)}
              style={{ width: 300 }}
            />
          </Group>
        </Card>

        {/* Purchase Invoices Table */}
        <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
          <Table.ScrollContainer minWidth={850}>
            <Table striped highlightOnHover verticalSpacing="sm" fz="sm">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Voucher #</Table.Th>
                  <Table.Th>Supplier Bill #</Table.Th>
                  <Table.Th>Date</Table.Th>
                  <Table.Th>Supplier</Table.Th>
                  <Table.Th ta="right">Taxable</Table.Th>
                  <Table.Th ta="right">GST</Table.Th>
                  <Table.Th ta="right">Total</Table.Th>
                  <Table.Th>Status</Table.Th>
                  <Table.Th ta="center">Actions</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {isLoading ? (
                  <Table.Tr>
                    <Table.Td colSpan={9} ta="center" py="xl">
                      Loading purchase bills...
                    </Table.Td>
                  </Table.Tr>
                ) : !invoicesData?.content || invoicesData.content.length === 0 ? (
                  <Table.Tr>
                    <Table.Td colSpan={9} ta="center" py="xl" c="dimmed">
                      No purchase bills found. Click &quot;New Purchase Bill&quot; to record one.
                    </Table.Td>
                  </Table.Tr>
                ) : (
                  invoicesData.content.map((bill: PurchaseInvoice) => {
                    const gstTotal =
                      Number(bill.cgstTotal || 0) + Number(bill.sgstTotal || 0) + Number(bill.igstTotal || 0);
                    return (
                      <Table.Tr key={bill.id}>
                        <Table.Td fw={700} c="teal.8">
                          {bill.voucherNo || 'DRAFT'}
                        </Table.Td>
                        <Table.Td fw={600}>{bill.supplierInvoiceNo}</Table.Td>
                        <Table.Td>{formatDate(bill.purchaseDate)}</Table.Td>
                        <Table.Td fw={600}>{bill.supplierName}</Table.Td>
                        <Table.Td ta="right">{formatCurrency(bill.taxableTotal)}</Table.Td>
                        <Table.Td ta="right">{formatCurrency(gstTotal)}</Table.Td>
                        <Table.Td ta="right" fw={800} c="dark.8">
                          {formatCurrency(bill.grandTotal)}
                        </Table.Td>
                        <Table.Td>
                          <Badge
                            size="sm"
                            color={
                              bill.status === 'POSTED'
                                ? 'teal'
                                : bill.status === 'REVERSED'
                                  ? 'red'
                                  : 'yellow'
                            }
                          >
                            {bill.status}
                          </Badge>
                        </Table.Td>
                        <Table.Td ta="center">
                          <Group gap={6} justify="center">
                            {bill.status === 'POSTED' && (
                              <ActionIcon
                                size="sm"
                                variant="light"
                                color="red"
                                title="Reverse / Cancel Bill"
                                onClick={() => {
                                  setSelectedInvoice(bill);
                                  setReversalReason('');
                                  openReverseModal();
                                }}
                              >
                                <IconRotateClockwise2 size={15} />
                              </ActionIcon>
                            )}
                            {bill.status === 'DRAFT' && (
                              <Button
                                size="xs"
                                variant="light"
                                color="teal"
                                leftSection={<IconCheck size={12} />}
                                onClick={() => postMutation.mutate(bill.id)}
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

      {/* Drawer */}
      <Drawer
        opened={drawerOpened}
        onClose={closeDrawer}
        title={
          <Text fw={700} size="md">
            New Purchase Inward Bill
          </Text>
        }
        position="right"
        size="70%"
      >
        <Stack gap="md">
          <SimpleGrid cols={{ base: 1, sm: 4 }}>
            <Select
              label="Supplier"
              placeholder="Select Supplier"
              required
              data={
                suppliers?.content.map((s: Supplier) => ({
                  value: String(s.id),
                  label: `${s.name} (${s.code})`,
                })) || []
              }
              value={supplierId}
              onChange={(val) => setSupplierId(val || '')}
              searchable
            />
            <TextInput
              label="Supplier Bill #"
              placeholder="e.g. INV-9821"
              required
              value={supplierInvoiceNo}
              onChange={(e) => setSupplierInvoiceNo(e.currentTarget.value)}
            />
            <TextInput
              label="Bill Date"
              type="date"
              required
              value={supplierInvoiceDate}
              onChange={(e) => setSupplierInvoiceDate(e.currentTarget.value)}
            />
            <TextInput
              label="Inward / Purchase Date"
              type="date"
              required
              value={purchaseDate}
              onChange={(e) => setPurchaseDate(e.currentTarget.value)}
            />
          </SimpleGrid>

          <Divider label="Purchased Items" labelPosition="center" />

          <Table withTableBorder withColumnBorders fz="xs">
            <Table.Thead bg="gray.1">
              <Table.Tr>
                <Table.Th style={{ width: '30%' }}>Product / Yarn</Table.Th>
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

          <Button variant="light" color="teal" size="xs" onClick={handleAddLine} style={{ alignSelf: 'flex-start' }}>
            + Add Another Line
          </Button>

          <SimpleGrid cols={{ base: 1, sm: 2 }} mt="md">
            <TextInput
              label="Purchase Notes / Vehicle Details"
              placeholder="e.g. Inward via Lorry TN-33-AB-1234"
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
                  <Text fw={800} size="md" c="teal.8">
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
              color="teal"
              loading={createDraftMutation.isPending}
              onClick={() => createDraftMutation.mutate(false)}
            >
              Save as Draft
            </Button>
            <Button
              color="teal"
              loading={createDraftMutation.isPending}
              onClick={() => createDraftMutation.mutate(true)}
            >
              Post &amp; Finalize Bill
            </Button>
          </Group>
        </Stack>
      </Drawer>

      {/* Reverse Modal */}
      <Modal opened={reverseModalOpened} onClose={closeReverseModal} title="Confirm Purchase Cancellation">
        <Stack gap="md">
          <Text size="sm">
            Are you sure you want to reverse voucher <b>{selectedInvoice?.voucherNo}</b> (Invoice #{' '}
            {selectedInvoice?.supplierInvoiceNo})?
          </Text>
          <Text size="xs" c="dimmed">
            An exact mirror reversal Journal Entry will be posted to restore supplier balances and GST input accounts.
          </Text>
          <TextInput
            label="Reversal Reason"
            placeholder="e.g., Material rejected or invoice entered in duplicate"
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
    </Container>
  );
};
