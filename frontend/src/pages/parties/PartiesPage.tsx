import React, { useState } from 'react';
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
  Stack,
  Tabs,
  SimpleGrid,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  IconPlus,
  IconEdit,
  IconUsers,
  IconBuildingStore,
  IconFileText,
  IconSearch,
} from '@tabler/icons-react';
import { textileApi, Customer, Supplier, GstState, PartyLedgerLine } from '../../api/textileApi';
import { formatCurrency, formatDate } from '../../utils/formatters';

export const PartiesPage: React.FC = () => {
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<string>('customers');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const [drawerOpened, { open: openDrawer, close: closeDrawer }] = useDisclosure(false);
  const [ledgerModalOpened, { open: openLedgerModal, close: closeLedgerModal }] = useDisclosure(false);

  const [editingPartyId, setEditingPartyId] = useState<number | null>(null);
  const [selectedParty, setSelectedParty] = useState<{ id: number; name: string } | null>(null);

  // Form State
  const [name, setName] = useState<string>('');
  const [contactPerson, setContactPerson] = useState<string>('');
  const [phone, setPhone] = useState<string>('');
  const [email, setEmail] = useState<string>('');
  const [address, setAddress] = useState<string>('');
  const [city, setCity] = useState<string>('');
  const [stateCode, setStateCode] = useState<string>('33');
  const [gstin, setGstin] = useState<string>('');
  const [pan, setPan] = useState<string>('');
  const [notes, setNotes] = useState<string>('');

  // Queries
  const { data: customersData, isLoading: customersLoading } = useQuery({
    queryKey: ['customers', searchQuery],
    queryFn: () => textileApi.getCustomers({ q: searchQuery || undefined, size: 50 }),
    enabled: activeTab === 'customers',
  });

  const { data: suppliersData, isLoading: suppliersLoading } = useQuery({
    queryKey: ['suppliers', searchQuery],
    queryFn: () => textileApi.getSuppliers({ q: searchQuery || undefined, size: 50 }),
    enabled: activeTab === 'suppliers',
  });

  const { data: gstStates } = useQuery({
    queryKey: ['gstStates'],
    queryFn: textileApi.getGstStates,
  });

  const { data: ledgerLines, isLoading: ledgerLoading } = useQuery<PartyLedgerLine[]>({
    queryKey: ['partyLedger', activeTab, selectedParty?.id],
    queryFn: () => {
      if (activeTab === 'customers') {
        return textileApi.getCustomerLedger(selectedParty!.id);
      } else {
        return textileApi.getSupplierLedger(selectedParty!.id);
      }
    },
    enabled: selectedParty !== null,
  });

  // When GSTIN is typed (15 chars), auto-detect state code from first 2 chars
  const handleGstinChange = (val: string) => {
    const clean = val.toUpperCase().trim();
    setGstin(clean);
    if (clean.length >= 2) {
      const detectedState = clean.substring(0, 2);
      if (gstStates?.some((s) => s.code === detectedState)) {
        setStateCode(detectedState);
      }
    }
    if (clean.length >= 12 && !pan) {
      setPan(clean.substring(2, 12));
    }
  };

  const handleOpenCreate = () => {
    setEditingPartyId(null);
    setName('');
    setContactPerson('');
    setPhone('');
    setEmail('');
    setAddress('');
    setCity('');
    setStateCode('33');
    setGstin('');
    setPan('');
    setNotes('');
    openDrawer();
  };

  const handleOpenEdit = (p: Customer | Supplier) => {
    setEditingPartyId(p.id);
    setName(p.name);
    setContactPerson(p.contactPerson || '');
    setPhone(p.phone || '');
    setEmail(p.email || '');
    setAddress(p.address || '');
    setCity(p.city || '');
    setStateCode(p.stateCode);
    setGstin(p.gstin || '');
    setPan(p.pan || '');
    setNotes(p.notes || '');
    openDrawer();
  };

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = {
        name,
        contactPerson,
        phone,
        email,
        address,
        city,
        stateCode,
        gstin: gstin || undefined,
        pan: pan || undefined,
        notes,
      };
      if (activeTab === 'customers') {
        return editingPartyId
          ? textileApi.updateCustomer(editingPartyId, payload)
          : textileApi.createCustomer(payload);
      } else {
        return editingPartyId
          ? textileApi.updateSupplier(editingPartyId, payload)
          : textileApi.createSupplier(payload);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [activeTab] });
      closeDrawer();
    },
  });

  return (
    <Container size="xl" py="md">
      <Stack gap="lg">
        {/* Header */}
        <Group justify="space-between" align="center">
          <div>
            <Title order={2} fw={800} c="dark.8">
              Parties &amp; GST Profiles
            </Title>
            <Text size="sm" c="dimmed">
              Master registry of Customers (Receivables) and Suppliers (Payables) with state GSTIN validation
            </Text>
          </div>
          <Button
            color={activeTab === 'customers' ? 'indigo' : 'teal'}
            size="sm"
            leftSection={<IconPlus size={16} />}
            onClick={handleOpenCreate}
          >
            {activeTab === 'customers' ? '+ Add Customer' : '+ Add Supplier'}
          </Button>
        </Group>

        {/* Tab Switcher & Search */}
        <Card shadow="xs" padding="sm" radius="md" withBorder bg="white">
          <Group justify="space-between" wrap="wrap">
            <Tabs value={activeTab} onChange={(val) => setActiveTab(val || 'customers')}>
              <Tabs.List>
                <Tabs.Tab value="customers" leftSection={<IconUsers size={16} />}>
                  Customers (Buyers)
                </Tabs.Tab>
                <Tabs.Tab value="suppliers" leftSection={<IconBuildingStore size={16} />}>
                  Suppliers (Yarn &amp; Mills)
                </Tabs.Tab>
              </Tabs.List>
            </Tabs>

            <TextInput
              placeholder={`Search ${activeTab} by name, code, GSTIN...`}
              size="xs"
              leftSection={<IconSearch size={14} />}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.currentTarget.value)}
              style={{ width: 280 }}
            />
          </Group>
        </Card>

        {/* Parties Table */}
        <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
          <Table.ScrollContainer minWidth={750}>
            <Table striped highlightOnHover verticalSpacing="sm" fz="sm">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Code</Table.Th>
                  <Table.Th>Name</Table.Th>
                  <Table.Th>GSTIN / State</Table.Th>
                  <Table.Th>City / Address</Table.Th>
                  <Table.Th>Phone</Table.Th>
                  <Table.Th>Status</Table.Th>
                  <Table.Th ta="center">Actions</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {customersLoading || suppliersLoading ? (
                  <Table.Tr>
                    <Table.Td colSpan={7} ta="center" py="xl">
                      Loading parties...
                    </Table.Td>
                  </Table.Tr>
                ) : (
                  (activeTab === 'customers' ? customersData?.content : suppliersData?.content)?.map(
                    (p: Customer | Supplier) => (
                      <Table.Tr key={p.id}>
                        <Table.Td fw={700} c="indigo.8">
                          {p.code}
                        </Table.Td>
                        <Table.Td fw={600}>{p.name}</Table.Td>
                        <Table.Td>
                          {p.gstin ? (
                            <Badge size="xs" variant="outline" color="blue">
                              {p.gstin} ({p.stateCode})
                            </Badge>
                          ) : (
                            <Text size="xs" c="dimmed">
                              Unregistered ({p.stateCode})
                            </Text>
                          )}
                        </Table.Td>
                        <Table.Td>{p.city ? `${p.city}, ${p.stateName}` : p.stateName}</Table.Td>
                        <Table.Td>{p.phone || '-'}</Table.Td>
                        <Table.Td>
                          <Badge size="xs" color={p.active ? 'teal' : 'gray'}>
                            {p.active ? 'Active' : 'Inactive'}
                          </Badge>
                        </Table.Td>
                        <Table.Td ta="center">
                          <Group gap={6} justify="center">
                            <ActionIcon
                              size="sm"
                              variant="light"
                              color="indigo"
                              title="Party Ledger Statement"
                              onClick={() => {
                                setSelectedParty({ id: p.id, name: p.name });
                                openLedgerModal();
                              }}
                            >
                              <IconFileText size={15} />
                            </ActionIcon>
                            <ActionIcon
                              size="sm"
                              variant="light"
                              color="blue"
                              title="Edit Party"
                              onClick={() => handleOpenEdit(p)}
                            >
                              <IconEdit size={15} />
                            </ActionIcon>
                          </Group>
                        </Table.Td>
                      </Table.Tr>
                    )
                  )
                )}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </Card>
      </Stack>

      {/* Add / Edit Party Drawer */}
      <Drawer
        opened={drawerOpened}
        onClose={closeDrawer}
        title={
          <Text fw={700} size="md">
            {editingPartyId ? `Edit ${activeTab.slice(0, -1)}` : `New ${activeTab.slice(0, -1)}`}
          </Text>
        }
        position="right"
        size="md"
      >
        <Stack gap="sm">
          <TextInput
            label="Party Name"
            placeholder="e.g. Arun Textiles / Lakshmi Spinners"
            required
            value={name}
            onChange={(e) => setName(e.currentTarget.value)}
          />

          <TextInput
            label="Contact Person"
            placeholder="e.g. Mr. K. Murugan"
            value={contactPerson}
            onChange={(e) => setContactPerson(e.currentTarget.value)}
          />

          <SimpleGrid cols={2}>
            <TextInput
              label="Phone"
              placeholder="e.g. 9842100000"
              value={phone}
              onChange={(e) => setPhone(e.currentTarget.value)}
            />
            <TextInput
              label="Email"
              placeholder="e.g. accounts@party.com"
              value={email}
              onChange={(e) => setEmail(e.currentTarget.value)}
            />
          </SimpleGrid>

          <SimpleGrid cols={2}>
            <TextInput
              label="GSTIN (15 characters)"
              placeholder="e.g. 33AAAAA0000A1Z5"
              maxLength={15}
              value={gstin}
              onChange={(e) => handleGstinChange(e.currentTarget.value)}
            />
            <Select
              label="State (Place of Supply)"
              placeholder="Select State"
              required
              data={
                gstStates?.map((s: GstState) => ({
                  value: s.code,
                  label: `${s.code} - ${s.name}`,
                })) || []
              }
              value={stateCode}
              onChange={(val) => setStateCode(val || '33')}
              searchable
            />
          </SimpleGrid>

          <SimpleGrid cols={2}>
            <TextInput
              label="PAN"
              placeholder="e.g. AAAAA0000A"
              maxLength={10}
              value={pan}
              onChange={(e) => setPan(e.currentTarget.value.toUpperCase())}
            />
            <TextInput
              label="City"
              placeholder="e.g. Tiruppur / Surat"
              value={city}
              onChange={(e) => setCity(e.currentTarget.value)}
            />
          </SimpleGrid>

          <TextInput
            label="Street Address"
            placeholder="Building, street, pincode"
            value={address}
            onChange={(e) => setAddress(e.currentTarget.value)}
          />

          <TextInput
            label="Notes / Credit Period"
            placeholder="e.g. 30 days payment credit terms"
            value={notes}
            onChange={(e) => setNotes(e.currentTarget.value)}
          />

          <Group justify="flex-end" mt="xl">
            <Button variant="default" onClick={closeDrawer}>
              Cancel
            </Button>
            <Button
              color={activeTab === 'customers' ? 'indigo' : 'teal'}
              onClick={() => saveMutation.mutate()}
              loading={saveMutation.isPending}
              disabled={!name.trim() || !stateCode}
            >
              Save Party
            </Button>
          </Group>
        </Stack>
      </Drawer>

      {/* Ledger Statement Modal */}
      <Modal
        opened={ledgerModalOpened}
        onClose={closeLedgerModal}
        title={`Ledger Account: ${selectedParty?.name}`}
        size="lg"
      >
        <Stack gap="sm">
          {ledgerLoading ? (
            <Text py="lg" ta="center">
              Loading ledger statement...
            </Text>
          ) : !ledgerLines || ledgerLines.length === 0 ? (
            <Text py="lg" ta="center" c="dimmed">
              No transactions recorded for this party.
            </Text>
          ) : (
            <Table.ScrollContainer minWidth={500}>
              <Table striped highlightOnHover verticalSpacing="xs" fz="xs">
                <Table.Thead bg="gray.1">
                  <Table.Tr>
                    <Table.Th>Date</Table.Th>
                    <Table.Th>Voucher #</Table.Th>
                    <Table.Th>Particulars / Narration</Table.Th>
                    <Table.Th ta="right">Debit (Dr)</Table.Th>
                    <Table.Th ta="right">Credit (Cr)</Table.Th>
                    <Table.Th ta="right">Net Balance</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {ledgerLines.map((l: PartyLedgerLine, idx: number) => (
                    <Table.Tr key={idx}>
                      <Table.Td>{formatDate(l.date)}</Table.Td>
                      <Table.Td fw={600}>{l.entryNo}</Table.Td>
                      <Table.Td>{l.description}</Table.Td>
                      <Table.Td ta="right">{Number(l.debit) > 0 ? formatCurrency(l.debit) : '-'}</Table.Td>
                      <Table.Td ta="right">{Number(l.credit) > 0 ? formatCurrency(l.credit) : '-'}</Table.Td>
                      <Table.Td ta="right" fw={700}>
                        {formatCurrency(l.runningBalance)}
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          )}
        </Stack>
      </Modal>
    </Container>
  );
};
