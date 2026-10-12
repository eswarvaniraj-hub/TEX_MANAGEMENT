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
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useLocation } from 'react-router-dom';
import {
  IconPlus,
  IconRotateClockwise2,
  IconReportMoney,
} from '@tabler/icons-react';
import { textileApi, ExpenseItem, ExpenseCategory, PaymentAccount } from '../../api/textileApi';
import { formatCurrency, formatDate } from '../../utils/formatters';

export const ExpensesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const location = useLocation();

  const [createModalOpened, { open: openCreateModal, close: closeCreateModal }] = useDisclosure(false);
  const [reverseModalOpened, { open: openReverseModal, close: closeReverseModal }] = useDisclosure(false);

  const [selectedExpense, setSelectedExpense] = useState<ExpenseItem | null>(null);
  const [reversalReason, setReversalReason] = useState<string>('');

  // Form State
  const [expenseDate, setExpenseDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [categoryId, setCategoryId] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  const [amount, setAmount] = useState<number>(0);
  const [paymentAccountId, setPaymentAccountId] = useState<string>('');
  const [referenceNo, setReferenceNo] = useState<string>('');
  const [notes, setNotes] = useState<string>('');

  useEffect(() => {
    if (location.search.includes('create=true')) {
      openCreateModal();
    }
  }, [location.search, openCreateModal]);

  const { data: expensesData, isLoading } = useQuery({
    queryKey: ['expensesList'],
    queryFn: () => textileApi.getExpenses({ size: 50 }),
  });

  const { data: categories } = useQuery({
    queryKey: ['expenseCategories'],
    queryFn: textileApi.getExpenseCategories,
  });

  const { data: paymentAccounts } = useQuery({
    queryKey: ['paymentAccountsList'],
    queryFn: textileApi.getPaymentAccounts,
  });

  useEffect(() => {
    if (categories && categories.length > 0 && !categoryId) {
      setCategoryId(String(categories[0].id));
    }
  }, [categories, categoryId]);

  useEffect(() => {
    if (paymentAccounts && paymentAccounts.length > 0 && !paymentAccountId) {
      setPaymentAccountId(String(paymentAccounts[0].id));
    }
  }, [paymentAccounts, paymentAccountId]);

  const createMutation = useMutation({
    mutationFn: () => {
      const payload = {
        expenseDate,
        categoryId: Number(categoryId),
        description,
        amount,
        paymentAccountId: Number(paymentAccountId),
        referenceNo,
        notes,
      };
      return textileApi.createExpense(payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expensesList'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      closeCreateModal();
      setDescription('');
      setAmount(0);
      setReferenceNo('');
      setNotes('');
    },
  });

  const reverseMutation = useMutation({
    mutationFn: () => textileApi.reverseExpense(selectedExpense!.id, reversalReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expensesList'] });
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
              Operating Expenses
            </Title>
            <Text size="sm" c="dimmed">
              Direct production expenses, factory maintenance, electricity, and administrative costs
            </Text>
          </div>
          <Button
            color="pink"
            size="sm"
            leftSection={<IconPlus size={16} />}
            onClick={() => {
              setDescription('');
              setAmount(0);
              openCreateModal();
            }}
          >
            Record Expense
          </Button>
        </Group>

        {/* Expenses Table */}
        <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
          <Table.ScrollContainer minWidth={800}>
            <Table striped highlightOnHover verticalSpacing="sm" fz="sm">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Voucher #</Table.Th>
                  <Table.Th>Date</Table.Th>
                  <Table.Th>Expense Category</Table.Th>
                  <Table.Th>Description</Table.Th>
                  <Table.Th>Paid From</Table.Th>
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
                      Loading expenses...
                    </Table.Td>
                  </Table.Tr>
                ) : !expensesData?.content || expensesData.content.length === 0 ? (
                  <Table.Tr>
                    <Table.Td colSpan={9} ta="center" py="xl" c="dimmed">
                      No expense vouchers recorded yet. Click &quot;Record Expense&quot; to begin.
                    </Table.Td>
                  </Table.Tr>
                ) : (
                  expensesData.content.map((e: ExpenseItem) => (
                    <Table.Tr key={e.id}>
                      <Table.Td fw={700} c="pink.8">
                        {e.expenseNo}
                      </Table.Td>
                      <Table.Td>{formatDate(e.expenseDate)}</Table.Td>
                      <Table.Td fw={600}>{e.categoryName}</Table.Td>
                      <Table.Td>{e.description}</Table.Td>
                      <Table.Td>{e.paymentAccountName}</Table.Td>
                      <Table.Td>{e.referenceNo || '-'}</Table.Td>
                      <Table.Td ta="right" fw={800} c="dark.8">
                        {formatCurrency(e.amount)}
                      </Table.Td>
                      <Table.Td>
                        <Badge size="sm" color={e.status === 'POSTED' ? 'teal' : 'red'}>
                          {e.status}
                        </Badge>
                      </Table.Td>
                      <Table.Td ta="center">
                        {e.status === 'POSTED' && (
                          <ActionIcon
                            size="sm"
                            variant="light"
                            color="red"
                            title="Reverse Expense"
                            onClick={() => {
                              setSelectedExpense(e);
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

      {/* Record Expense Modal */}
      <Modal
        opened={createModalOpened}
        onClose={closeCreateModal}
        title={
          <Group gap="xs">
            <IconReportMoney size={20} color="#db2777" />
            <Text fw={700} size="md">
              Record Operational Expense
            </Text>
          </Group>
        }
        size="md"
      >
        <Stack gap="sm">
          <TextInput
            label="Expense Date"
            type="date"
            required
            value={expenseDate}
            onChange={(e) => setExpenseDate(e.currentTarget.value)}
          />

          <Select
            label="Expense Category"
            placeholder="Select Category"
            required
            data={
              categories?.map((c: ExpenseCategory) => ({
                value: String(c.id),
                label: `${c.name} (${c.accountCode})`,
              })) || []
            }
            value={categoryId}
            onChange={(val) => setCategoryId(val || '')}
            searchable
          />

          <TextInput
            label="Description / Purpose"
            placeholder="e.g. EB Power Bill for Weaving Shed Unit 1"
            required
            value={description}
            onChange={(e) => setDescription(e.currentTarget.value)}
          />

          <NumberInput
            label="Amount (₹)"
            min={0.01}
            required
            value={amount}
            onChange={(val) => setAmount(Number(val) || 0)}
          />

          <Select
            label="Paid From Account"
            placeholder="Select Cash / Bank Account"
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

          <TextInput
            label="Voucher / Bill # / Reference"
            placeholder="e.g. EB Bill # 928372"
            value={referenceNo}
            onChange={(e) => setReferenceNo(e.currentTarget.value)}
          />

          <TextInput
            label="Notes"
            placeholder="Optional remarks"
            value={notes}
            onChange={(e) => setNotes(e.currentTarget.value)}
          />

          <Group justify="flex-end" mt="md">
            <Button variant="default" onClick={closeCreateModal}>
              Cancel
            </Button>
            <Button
              color="pink"
              onClick={() => createMutation.mutate()}
              loading={createMutation.isPending}
              disabled={!categoryId || !description || !amount || amount <= 0}
            >
              Post Expense
            </Button>
          </Group>
        </Stack>
      </Modal>

      {/* Reverse Modal */}
      <Modal opened={reverseModalOpened} onClose={closeReverseModal} title="Confirm Expense Reversal">
        <Stack gap="md">
          <Text size="sm">
            Are you sure you want to reverse expense voucher <b>{selectedExpense?.expenseNo}</b> (₹{' '}
            {selectedExpense?.amount})?
          </Text>
          <Text size="xs" c="dimmed">
            An exact mirror reversal Journal Entry will be posted to credit the expense account and debit the payment
            account.
          </Text>
          <TextInput
            label="Reversal Reason"
            placeholder="e.g., Recorded in duplicate or wrong account"
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
