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
  TextInput,
  Select,
  NumberInput,
  Stack,
  Tabs,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useLocation } from 'react-router-dom';
import {
  IconPlus,
  IconRotateClockwise2,
  IconArrowDownLeft,
  IconArrowUpRight,
} from '@tabler/icons-react';
import { textileApi, PaymentItem, Customer, Supplier, PaymentAccount } from '../../api/textileApi';
import { formatCurrency, formatDate } from '../../utils/formatters';

export const PaymentsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const location = useLocation();

  const [activeTab, setActiveTab] = useState<string>('RECEIPT');
  const [createModalOpened, { open: openCreateModal, close: closeCreateModal }] = useDisclosure(false);
  const [reverseModalOpened, { open: openReverseModal, close: closeReverseModal }] = useDisclosure(false);

  const [selectedPayment, setSelectedPayment] = useState<PaymentItem | null>(null);
  const [reversalReason, setReversalReason] = useState<string>('');

  // Form State
  const [partyId, setPartyId] = useState<string>('');
  const [paymentDate, setPaymentDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [paymentAccountId, setPaymentAccountId] = useState<string>('');
  const [amount, setAmount] = useState<number>(0);
  const [method, setMethod] = useState<string>('BANK_TRANSFER');
  const [referenceNo, setReferenceNo] = useState<string>('');
  const [notes, setNotes] = useState<string>('');

  // Handle URL query parameters (e.g. ?tab=receipts&create=true)
  useEffect(() => {
    if (location.search.includes('tab=payments')) {
      setActiveTab('PAYMENT');
    } else if (location.search.includes('tab=receipts')) {
      setActiveTab('RECEIPT');
    }
    if (location.search.includes('create=true')) {
      openCreateModal();
    }
  }, [location.search, openCreateModal]);

  // Queries
  const { data: paymentsData, isLoading } = useQuery({
    queryKey: ['paymentsList', activeTab],
    queryFn: () => textileApi.getPayments({ direction: activeTab, size: 50 }),
  });

  const { data: customers } = useQuery({
    queryKey: ['customersList'],
    queryFn: () => textileApi.getCustomers({ size: 100 }),
    enabled: activeTab === 'RECEIPT',
  });

  const { data: suppliers } = useQuery({
    queryKey: ['suppliersList'],
    queryFn: () => textileApi.getSuppliers({ size: 100 }),
    enabled: activeTab === 'PAYMENT',
  });

  const { data: paymentAccounts } = useQuery({
    queryKey: ['paymentAccountsList'],
    queryFn: textileApi.getPaymentAccounts,
  });

  useEffect(() => {
    if (paymentAccounts && paymentAccounts.length > 0 && !paymentAccountId) {
      setPaymentAccountId(String(paymentAccounts[0].id));
    }
  }, [paymentAccounts, paymentAccountId]);

  // Mutations
  const createMutation = useMutation({
    mutationFn: () => {
      const payload = {
        direction: activeTab,
        customerId: activeTab === 'RECEIPT' ? Number(partyId) : null,
        supplierId: activeTab === 'PAYMENT' ? Number(partyId) : null,
        paymentDate,
        paymentAccountId: Number(paymentAccountId),
        amount,
        method,
        referenceNo,
        notes,
      };
      return textileApi.createPayment(payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['paymentsList'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      closeCreateModal();
      setAmount(0);
      setReferenceNo('');
      setNotes('');
    },
  });

  const reverseMutation = useMutation({
    mutationFn: () => textileApi.reversePayment(selectedPayment!.id, reversalReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['paymentsList'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      closeReverseModal();
    },
  });

  return (
    <Container size="xl" py="md">
      <Stack gap="lg">
        {/* Header */}
        <Group justify="space-between" align="center">
          <div>
            <Title order={2} fw={800} c="dark.8">
              Payments &amp; Receipts
            </Title>
            <Text size="sm" c="dimmed">
              Internal accounting entries for customer collections and supplier bill settlements
            </Text>
          </div>
          <Button
            color={activeTab === 'RECEIPT' ? 'indigo' : 'orange'}
            size="sm"
            leftSection={<IconPlus size={16} />}
            onClick={() => {
              setPartyId('');
              setAmount(0);
              openCreateModal();
            }}
          >
            {activeTab === 'RECEIPT' ? '+ Record Customer Receipt' : '+ Record Supplier Payment'}
          </Button>
        </Group>

        {/* Direction Tabs */}
        <Tabs value={activeTab} onChange={(val) => setActiveTab(val || 'RECEIPT')}>
          <Tabs.List>
            <Tabs.Tab value="RECEIPT" leftSection={<IconArrowDownLeft size={16} color="#4338ca" />}>
              Customer Receipts (Money In)
            </Tabs.Tab>
            <Tabs.Tab value="PAYMENT" leftSection={<IconArrowUpRight size={16} color="#ea580c" />}>
              Supplier Payments (Money Out)
            </Tabs.Tab>
          </Tabs.List>
        </Tabs>

        {/* Payments Table */}
        <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
          <Table.ScrollContainer minWidth={800}>
            <Table striped highlightOnHover verticalSpacing="sm" fz="sm">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Voucher #</Table.Th>
                  <Table.Th>Date</Table.Th>
                  <Table.Th>{activeTab === 'RECEIPT' ? 'Customer' : 'Supplier'}</Table.Th>
                  <Table.Th>Payment Account</Table.Th>
                  <Table.Th>Method</Table.Th>
                  <Table.Th>Reference</Table.Th>
                  <Table.Th ta="right">Amount</Table.Th>
                  <Table.Th>Status</Table.Th>
                  <Table.Th ta="center">Actions</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {isLoading ? (
                  <Table.Tr>
                    <Table.Td colSpan={9} ta="center" py="xl">
                      Loading vouchers...
                    </Table.Td>
                  </Table.Tr>
                ) : !paymentsData?.content || paymentsData.content.length === 0 ? (
                  <Table.Tr>
                    <Table.Td colSpan={9} ta="center" py="xl" c="dimmed">
                      No vouchers found. Click the button above to record a new transaction.
                    </Table.Td>
                  </Table.Tr>
                ) : (
                  paymentsData.content.map((p: PaymentItem) => (
                    <Table.Tr key={p.id}>
                      <Table.Td fw={700} c={activeTab === 'RECEIPT' ? 'indigo.8' : 'orange.8'}>
                        {p.paymentNo}
                      </Table.Td>
                      <Table.Td>{formatDate(p.paymentDate)}</Table.Td>
                      <Table.Td fw={600}>
                        {activeTab === 'RECEIPT' ? p.customerName : p.supplierName}
                      </Table.Td>
                      <Table.Td>{p.paymentAccountName}</Table.Td>
                      <Table.Td>
                        <Badge size="xs" variant="outline" color="gray">
                          {p.method}
                        </Badge>
                      </Table.Td>
                      <Table.Td>{p.referenceNo || '-'}</Table.Td>
                      <Table.Td ta="right" fw={800} c="dark.8">
                        {formatCurrency(p.amount)}
                      </Table.Td>
                      <Table.Td>
                        <Badge size="sm" color={p.status === 'POSTED' ? 'teal' : 'red'}>
                          {p.status}
                        </Badge>
                      </Table.Td>
                      <Table.Td ta="center">
                        {p.status === 'POSTED' && (
                          <ActionIcon
                            size="sm"
                            variant="light"
                            color="red"
                            title="Reverse Payment"
                            onClick={() => {
                              setSelectedPayment(p);
                              setReversalReason('');
                              openReverseModal();
                            }}
                          >
                            <IconRotateClockwise2 size={15} />
                          </ActionIcon>
                        )}
                      </Table.Td>
                    </Table.Tr>
                  ))
                )}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </Card>
      </Stack>

      {/* Record Payment / Receipt Modal */}
      <Modal
        opened={createModalOpened}
        onClose={closeCreateModal}
        title={
          <Text fw={700} size="md">
            {activeTab === 'RECEIPT' ? 'Record Customer Receipt' : 'Record Supplier Payment'}
          </Text>
        }
        size="md"
      >
        <Stack gap="sm">
          <Select
            label={activeTab === 'RECEIPT' ? 'Customer' : 'Supplier'}
            placeholder="Select Party"
            required
            data={
              activeTab === 'RECEIPT'
                ? customers?.content.map((c: Customer) => ({
                    value: String(c.id),
                    label: `${c.name} (${c.code})`,
                  })) || []
                : suppliers?.content.map((s: Supplier) => ({
                    value: String(s.id),
                    label: `${s.name} (${s.code})`,
                  })) || []
            }
            value={partyId}
            onChange={(val) => setPartyId(val || '')}
            searchable
          />

          <TextInput
            label="Transaction Date"
            type="date"
            required
            value={paymentDate}
            onChange={(e) => setPaymentDate(e.currentTarget.value)}
          />

          <Select
            label="Payment Account (Cash / Bank)"
            placeholder="Select Account"
            required
            data={
              paymentAccounts?.map((pa: PaymentAccount) => ({
                value: String(pa.id),
                label: `${pa.name} (${pa.kind})`,
              })) || []
            }
            value={paymentAccountId}
            onChange={(val) => setPaymentAccountId(val || '')}
          />

          <NumberInput
            label="Amount (₹)"
            min={0.01}
            required
            value={amount}
            onChange={(val) => setAmount(Number(val) || 0)}
          />

          <Select
            label="Payment Mode"
            required
            data={[
              { value: 'BANK_TRANSFER', label: 'NEFT / RTGS / IMPS' },
              { value: 'UPI', label: 'UPI / GPay / PhonePe' },
              { value: 'CASH', label: 'Cash in Hand' },
              { value: 'CHEQUE', label: 'Cheque' },
              { value: 'OTHER', label: 'Other Internal Voucher' },
            ]}
            value={method}
            onChange={(val) => setMethod(val || 'BANK_TRANSFER')}
          />

          <TextInput
            label="Reference # / Cheque # / UTR #"
            placeholder="e.g. UTR-98218732"
            value={referenceNo}
            onChange={(e) => setReferenceNo(e.currentTarget.value)}
          />

          <TextInput
            label="Notes"
            placeholder="e.g. On-account advance payment"
            value={notes}
            onChange={(e) => setNotes(e.currentTarget.value)}
          />

          <Group justify="flex-end" mt="md">
            <Button variant="default" onClick={closeCreateModal}>
              Cancel
            </Button>
            <Button
              color={activeTab === 'RECEIPT' ? 'indigo' : 'orange'}
              onClick={() => createMutation.mutate()}
              loading={createMutation.isPending}
              disabled={!partyId || !amount || amount <= 0}
            >
              Post Voucher
            </Button>
          </Group>
        </Stack>
      </Modal>

      {/* Reverse Modal */}
      <Modal opened={reverseModalOpened} onClose={closeReverseModal} title="Confirm Voucher Reversal">
        <Stack gap="md">
          <Text size="sm">
            Are you sure you want to reverse voucher <b>{selectedPayment?.paymentNo}</b> (₹ {selectedPayment?.amount}
            )?
          </Text>
          <Text size="xs" c="dimmed">
            An exact mirror reversal Journal Entry will be posted to restore ledger account and party balances.
          </Text>
          <TextInput
            label="Reversal Reason"
            placeholder="e.g., Cheque bounced or voucher recorded in wrong account"
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
