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
  TextInput,
  Select,
  NumberInput,
  Stack,
  SimpleGrid,
  ThemeIcon,
  Divider,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  IconPlus,
  IconArrowsExchange,
  IconBuildingBank,
  IconCash,
  IconRotateClockwise2,
  IconFileText,
} from '@tabler/icons-react';
import { textileApi, PaymentAccount, FundTransferItem } from '../../api/textileApi';
import { formatCurrency, formatDate } from '../../utils/formatters';

export const BankingPage: React.FC = () => {
  const queryClient = useQueryClient();

  const [transferModalOpened, { open: openTransferModal, close: closeTransferModal }] = useDisclosure(false);
  const [accountModalOpened, { open: openAccountModal, close: closeAccountModal }] = useDisclosure(false);
  const [statementModalOpened, { open: openStatementModal, close: closeStatementModal }] = useDisclosure(false);
  const [reverseModalOpened, { open: openReverseModal, close: closeReverseModal }] = useDisclosure(false);

  const [selectedTransfer, setSelectedTransfer] = useState<FundTransferItem | null>(null);
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(null);
  const [selectedAccountName, setSelectedAccountName] = useState<string>('');
  const [reversalReason, setReversalReason] = useState<string>('');

  // Transfer Form State
  const [transferDate, setTransferDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [fromAccountId, setFromAccountId] = useState<string>('');
  const [toAccountId, setToAccountId] = useState<string>('');
  const [amount, setAmount] = useState<number>(0);
  const [reference, setReference] = useState<string>('');
  const [notes, setNotes] = useState<string>('');

  // Add Account Form State
  const [newAccName, setNewAccName] = useState<string>('');
  const [newAccKind, setNewAccKind] = useState<'CASH' | 'BANK'>('BANK');
  const [newBankName, setNewBankName] = useState<string>('');
  const [newLast4, setNewLast4] = useState<string>('');

  // Queries
  const { data: accounts, isLoading: accountsLoading } = useQuery({
    queryKey: ['paymentAccountsList'],
    queryFn: textileApi.getPaymentAccounts,
  });

  const { data: transfersData, isLoading: transfersLoading } = useQuery({
    queryKey: ['fundTransfersList'],
    queryFn: () => textileApi.getFundTransfers({ size: 50 }),
  });

  const { data: statementData, isLoading: statementLoading } = useQuery({
    queryKey: ['accountStatement', selectedAccountId],
    queryFn: () => textileApi.getAccountStatement(selectedAccountId!),
    enabled: selectedAccountId !== null,
  });

  // Mutations
  const transferMutation = useMutation({
    mutationFn: () =>
      textileApi.createFundTransfer({
        transferDate,
        fromPaymentAccountId: Number(fromAccountId),
        toPaymentAccountId: Number(toAccountId),
        amount,
        reference,
        notes,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['paymentAccountsList'] });
      queryClient.invalidateQueries({ queryKey: ['fundTransfersList'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      closeTransferModal();
      setAmount(0);
      setReference('');
      setNotes('');
    },
  });

  const createAccountMutation = useMutation({
    mutationFn: () =>
      textileApi.createPaymentAccount({
        name: newAccName,
        kind: newAccKind,
        bankName: newAccKind === 'BANK' ? newBankName : undefined,
        accountLast4: newAccKind === 'BANK' ? newLast4 : undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['paymentAccountsList'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      closeAccountModal();
      setNewAccName('');
      setNewBankName('');
      setNewLast4('');
    },
  });

  const reverseTransferMutation = useMutation({
    mutationFn: () => textileApi.reverseFundTransfer(selectedTransfer!.id, reversalReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['paymentAccountsList'] });
      queryClient.invalidateQueries({ queryKey: ['fundTransfersList'] });
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
              Banking &amp; Cash Accounts
            </Title>
            <Text size="sm" c="dimmed">
              Internal cashbook register, bank balances, contra transfers, and statement inquiries
            </Text>
          </div>
          <Group gap="xs">
            <Button
              variant="outline"
              color="indigo"
              size="sm"
              leftSection={<IconPlus size={16} />}
              onClick={openAccountModal}
            >
              Add Account
            </Button>
            <Button
              color="indigo"
              size="sm"
              leftSection={<IconArrowsExchange size={16} />}
              onClick={() => {
                if (accounts && accounts.length >= 2) {
                  setFromAccountId(String(accounts[0].id));
                  setToAccountId(String(accounts[1].id));
                }
                openTransferModal();
              }}
            >
              Transfer Funds (Contra)
            </Button>
          </Group>
        </Group>

        {/* Account Balances Cards */}
        <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="md">
          {accountsLoading ? (
            <Text>Loading payment accounts...</Text>
          ) : (
            accounts?.map((acc: PaymentAccount) => (
              <Card key={acc.id} shadow="xs" padding="md" radius="md" withBorder bg="white">
                <Group justify="space-between" mb="xs">
                  <ThemeIcon color={acc.kind === 'CASH' ? 'green' : 'blue'} variant="light" size="lg" radius="md">
                    {acc.kind === 'CASH' ? <IconCash size={22} /> : <IconBuildingBank size={22} />}
                  </ThemeIcon>
                  <Badge color={acc.kind === 'CASH' ? 'green' : 'blue'} variant="light" size="sm">
                    {acc.kind}
                  </Badge>
                </Group>
                <Text fw={700} size="md">
                  {acc.name}
                </Text>
                <Text size="xs" c="dimmed">
                  {acc.bankName ? `${acc.bankName} (ending in ${acc.accountLast4 || '****'})` : 'Cash Drawer / Register'}
                </Text>
                <Divider my="sm" />
                <Group justify="space-between" align="baseline">
                  <div>
                    <Text size="xs" c="dimmed">
                      Current Ledger Balance
                    </Text>
                    <Text fw={800} size="lg" c={Number(acc.currentBalance) >= 0 ? 'dark.8' : 'red.8'}>
                      {formatCurrency(acc.currentBalance)}
                    </Text>
                  </div>
                  <Button
                    variant="subtle"
                    size="xs"
                    color="indigo"
                    leftSection={<IconFileText size={14} />}
                    onClick={() => {
                      setSelectedAccountId(acc.id);
                      setSelectedAccountName(acc.name);
                      openStatementModal();
                    }}
                  >
                    Statement
                  </Button>
                </Group>
              </Card>
            ))
          )}
        </SimpleGrid>

        {/* Fund Transfers History Table */}
        <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
          <Group justify="space-between" mb="sm">
            <div>
              <Text fw={700} size="sm">
                Fund Transfer Journal History
              </Text>
              <Text size="xs" c="dimmed">
                Transfers between cash register and bank accounts (Contra entries)
              </Text>
            </div>
          </Group>

          <Table.ScrollContainer minWidth={750}>
            <Table striped highlightOnHover verticalSpacing="sm" fz="sm">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Transfer #</Table.Th>
                  <Table.Th>Date</Table.Th>
                  <Table.Th>Source (From)</Table.Th>
                  <Table.Th>Destination (To)</Table.Th>
                  <Table.Th>Reference</Table.Th>
                  <Table.Th ta="right">Amount</Table.Th>
                  <Table.Th>Status</Table.Th>
                  <Table.Th ta="center">Actions</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {transfersLoading ? (
                  <Table.Tr>
                    <Table.Td colSpan={8} ta="center" py="xl">
                      Loading transfers...
                    </Table.Td>
                  </Table.Tr>
                ) : !transfersData?.content || transfersData.content.length === 0 ? (
                  <Table.Tr>
                    <Table.Td colSpan={8} ta="center" py="xl" c="dimmed">
                      No fund transfers recorded yet. Click &quot;Transfer Funds&quot; to deposit or withdraw cash.
                    </Table.Td>
                  </Table.Tr>
                ) : (
                  transfersData.content.map((t: FundTransferItem) => (
                    <Table.Tr key={t.id}>
                      <Table.Td fw={700} c="indigo.8">
                        {t.transferNo}
                      </Table.Td>
                      <Table.Td>{formatDate(t.transferDate)}</Table.Td>
                      <Table.Td fw={600}>{t.fromPaymentAccountName}</Table.Td>
                      <Table.Td fw={600}>{t.toPaymentAccountName}</Table.Td>
                      <Table.Td>{t.reference || '-'}</Table.Td>
                      <Table.Td ta="right" fw={800} c="dark.8">
                        {formatCurrency(t.amount)}
                      </Table.Td>
                      <Table.Td>
                        <Badge size="sm" color={t.status === 'POSTED' ? 'teal' : 'red'}>
                          {t.status}
                        </Badge>
                      </Table.Td>
                      <Table.Td ta="center">
                        {t.status === 'POSTED' && (
                          <ActionIcon
                            size="sm"
                            variant="light"
                            color="red"
                            title="Reverse Transfer"
                            onClick={() => {
                              setSelectedTransfer(t);
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

      {/* Transfer Funds Modal */}
      <Modal opened={transferModalOpened} onClose={closeTransferModal} title="Record Contra Fund Transfer" size="md">
        <Stack gap="sm">
          <TextInput
            label="Transfer Date"
            type="date"
            required
            value={transferDate}
            onChange={(e) => setTransferDate(e.currentTarget.value)}
          />

          <Select
            label="Source Account (Transfer Out)"
            placeholder="From Account"
            required
            data={
              accounts?.map((a: PaymentAccount) => ({
                value: String(a.id),
                label: `${a.name} (${formatCurrency(a.currentBalance)})`,
              })) || []
            }
            value={fromAccountId}
            onChange={(val) => setFromAccountId(val || '')}
          />

          <Select
            label="Destination Account (Transfer In)"
            placeholder="To Account"
            required
            data={
              accounts?.map((a: PaymentAccount) => ({
                value: String(a.id),
                label: `${a.name} (${formatCurrency(a.currentBalance)})`,
              })) || []
            }
            value={toAccountId}
            onChange={(val) => setToAccountId(val || '')}
          />

          <NumberInput
            label="Amount (₹)"
            min={0.01}
            required
            value={amount}
            onChange={(val) => setAmount(Number(val) || 0)}
          />

          <TextInput
            label="Reference (e.g. Cheque / ATM Slip #)"
            placeholder="e.g. ATM-092812"
            value={reference}
            onChange={(e) => setReference(e.currentTarget.value)}
          />

          <TextInput
            label="Notes"
            placeholder="e.g. Cash withdrawal for factory wages"
            value={notes}
            onChange={(e) => setNotes(e.currentTarget.value)}
          />

          <Group justify="flex-end" mt="md">
            <Button variant="default" onClick={closeTransferModal}>
              Cancel
            </Button>
            <Button
              color="indigo"
              onClick={() => transferMutation.mutate()}
              loading={transferMutation.isPending}
              disabled={!fromAccountId || !toAccountId || fromAccountId === toAccountId || !amount || amount <= 0}
            >
              Post Transfer
            </Button>
          </Group>
        </Stack>
      </Modal>

      {/* Add Account Modal */}
      <Modal opened={accountModalOpened} onClose={closeAccountModal} title="Add Payment Account" size="sm">
        <Stack gap="sm">
          <TextInput
            label="Account Display Name"
            placeholder="e.g. HDFC Current Account"
            required
            value={newAccName}
            onChange={(e) => setNewAccName(e.currentTarget.value)}
          />
          <Select
            label="Kind"
            required
            data={[
              { value: 'BANK', label: 'Bank Account' },
              { value: 'CASH', label: 'Cash Drawer' },
            ]}
            value={newAccKind}
            onChange={(val) => setNewAccKind((val as 'CASH' | 'BANK') || 'BANK')}
          />
          {newAccKind === 'BANK' && (
            <>
              <TextInput
                label="Bank Name"
                placeholder="e.g. HDFC Bank"
                value={newBankName}
                onChange={(e) => setNewBankName(e.currentTarget.value)}
              />
              <TextInput
                label="Last 4 Digits of Account Number"
                placeholder="e.g. 8492"
                maxLength={4}
                value={newLast4}
                onChange={(e) => setNewLast4(e.currentTarget.value)}
              />
            </>
          )}
          <Group justify="flex-end" mt="md">
            <Button variant="default" onClick={closeAccountModal}>
              Cancel
            </Button>
            <Button
              color="indigo"
              onClick={() => createAccountMutation.mutate()}
              loading={createAccountMutation.isPending}
              disabled={!newAccName.trim()}
            >
              Save Account
            </Button>
          </Group>
        </Stack>
      </Modal>

      {/* Statement Modal */}
      <Modal
        opened={statementModalOpened}
        onClose={closeStatementModal}
        title={`Account Statement: ${selectedAccountName}`}
        size="lg"
      >
        <Stack gap="sm">
          {statementLoading ? (
            <Text py="lg" ta="center">
              Loading statement lines...
            </Text>
          ) : !statementData || statementData.length === 0 ? (
            <Text py="lg" ta="center" c="dimmed">
              No transactions recorded for this account.
            </Text>
          ) : (
            <Table.ScrollContainer minWidth={500}>
              <Table striped highlightOnHover verticalSpacing="xs" fz="xs">
                <Table.Thead bg="gray.1">
                  <Table.Tr>
                    <Table.Th>Date</Table.Th>
                    <Table.Th>JV #</Table.Th>
                    <Table.Th>Description</Table.Th>
                    <Table.Th ta="right">Debit (In)</Table.Th>
                    <Table.Th ta="right">Credit (Out)</Table.Th>
                    <Table.Th ta="right">Balance</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {statementData.map((s, idx) => (
                    <Table.Tr key={idx}>
                      <Table.Td>{formatDate(s.date)}</Table.Td>
                      <Table.Td fw={600}>{s.entryNo}</Table.Td>
                      <Table.Td>{s.description}</Table.Td>
                      <Table.Td ta="right">{Number(s.debit) > 0 ? formatCurrency(s.debit) : '-'}</Table.Td>
                      <Table.Td ta="right">{Number(s.credit) > 0 ? formatCurrency(s.credit) : '-'}</Table.Td>
                      <Table.Td ta="right" fw={700}>
                        {formatCurrency(s.runningBalance)}
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          )}
        </Stack>
      </Modal>

      {/* Reverse Modal */}
      <Modal opened={reverseModalOpened} onClose={closeReverseModal} title="Confirm Transfer Reversal">
        <Stack gap="md">
          <Text size="sm">
            Are you sure you want to reverse transfer <b>{selectedTransfer?.transferNo}</b> (₹{' '}
            {selectedTransfer?.amount})?
          </Text>
          <Text size="xs" c="dimmed">
            An exact mirror reversal Journal Entry will be posted to restore both account balances.
          </Text>
          <TextInput
            label="Reversal Reason"
            placeholder="e.g., Transfer entered with incorrect amount"
            required
            value={reversalReason}
            onChange={(e) => setReversalReason(e.currentTarget.value)}
          />
          <Group justify="flex-end">
            <Button variant="default" onClick={closeReverseModal}>
              Dismiss
            </Button>
            <Button color="red" onClick={() => reverseTransferMutation.mutate()} loading={reverseTransferMutation.isPending}>
              Confirm Reversal
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Container>
  );
};
