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
  Modal,
  TextInput,
  Select,
  Stack,
  Alert,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  IconPlus,
  IconShieldCheck,
  IconAlertTriangle,
  IconCheck,
} from '@tabler/icons-react';
import { textileApi, AccountItem, IntegrityCheckResult } from '../../api/textileApi';
import { formatCurrency } from '../../utils/formatters';

export const ChartOfAccountsPage: React.FC = () => {
  const queryClient = useQueryClient();

  const [addModalOpened, { open: openAddModal, close: closeAddModal }] = useDisclosure(false);
  const [integrityModalOpened, { open: openIntegrityModal, close: closeIntegrityModal }] = useDisclosure(false);

  // Form
  const [code, setCode] = useState<string>('');
  const [name, setName] = useState<string>('');
  const [accountType, setAccountType] = useState<string>('EXPENSE');
  const [reportGroup, setReportGroup] = useState<string>('OPERATING');

  const { data: accounts, isLoading } = useQuery({
    queryKey: ['accountsList'],
    queryFn: textileApi.getAccounts,
  });

  const { data: integrityResult, refetch: runIntegrityCheck, isFetching: integrityChecking } = useQuery<IntegrityCheckResult>({
    queryKey: ['integrityCheck'],
    queryFn: textileApi.getIntegrityCheck,
    enabled: false,
  });

  const createMutation = useMutation({
    mutationFn: () =>
      textileApi.createAccount({
        code,
        name,
        accountType,
        reportGroup,
        isPostable: true,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accountsList'] });
      closeAddModal();
      setCode('');
      setName('');
    },
  });

  const handleRunIntegrity = () => {
    runIntegrityCheck();
    openIntegrityModal();
  };

  return (
    <Container size="xl" py="md">
      <Stack gap="lg">
        {/* Header */}
        <Group justify="space-between" align="center">
          <div>
            <Title order={2} fw={800} c="dark.8">
              General Ledger &amp; Chart of Accounts
            </Title>
            <Text size="sm" c="dimmed">
              Double-entry single source of truth: 13 provisional system role accounts, balance computations, and integrity checks
            </Text>
          </div>
          <Group gap="xs">
            <Button
              variant="outline"
              color="teal"
              size="sm"
              leftSection={<IconShieldCheck size={16} />}
              onClick={handleRunIntegrity}
              loading={integrityChecking}
            >
              Verify Ledger Integrity
            </Button>
            <Button color="indigo" size="sm" leftSection={<IconPlus size={16} />} onClick={openAddModal}>
              + Add Account
            </Button>
          </Group>
        </Group>

        {/* Accounts Table */}
        <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
          <Table.ScrollContainer minWidth={750}>
            <Table striped highlightOnHover verticalSpacing="sm" fz="sm">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Code</Table.Th>
                  <Table.Th>Account Name</Table.Th>
                  <Table.Th>Type</Table.Th>
                  <Table.Th>Classification / System Role</Table.Th>
                  <Table.Th>Postable</Table.Th>
                  <Table.Th ta="right">Net Dynamic Balance</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {isLoading ? (
                  <Table.Tr>
                    <Table.Td colSpan={6} ta="center" py="xl">
                      Loading Chart of Accounts...
                    </Table.Td>
                  </Table.Tr>
                ) : (
                  accounts?.map((acc: AccountItem) => {
                    const typeColor =
                      acc.accountType === 'ASSET'
                        ? 'blue'
                        : acc.accountType === 'LIABILITY'
                          ? 'red'
                          : acc.accountType === 'INCOME'
                            ? 'teal'
                            : acc.accountType === 'EXPENSE'
                              ? 'pink'
                              : 'purple';
                    return (
                      <Table.Tr key={acc.id}>
                        <Table.Td fw={700} c="indigo.8">
                          {acc.code}
                        </Table.Td>
                        <Table.Td fw={600}>{acc.name}</Table.Td>
                        <Table.Td>
                          <Badge size="xs" color={typeColor}>
                            {acc.accountType}
                          </Badge>
                        </Table.Td>
                        <Table.Td>
                          {acc.systemRole ? (
                            <Badge size="xs" variant="outline" color="indigo">
                              {acc.systemRole}
                            </Badge>
                          ) : (
                            <Text size="xs" c="dimmed">
                              {acc.reportGroup || 'General'}
                            </Text>
                          )}
                        </Table.Td>
                        <Table.Td>
                          <Badge size="xs" variant="dot" color={acc.isPostable ? 'teal' : 'gray'}>
                            {acc.isPostable ? 'Postable' : 'Group Header'}
                          </Badge>
                        </Table.Td>
                        <Table.Td ta="right" fw={800} c="dark.8">
                          {formatCurrency(acc.balance)}
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

      {/* Add Account Modal */}
      <Modal opened={addModalOpened} onClose={closeAddModal} title="Add Ledger Account" size="sm">
        <Stack gap="sm">
          <TextInput
            label="Account Code"
            placeholder="e.g. 5250"
            required
            value={code}
            onChange={(e) => setCode(e.currentTarget.value.trim())}
          />
          <TextInput
            label="Account Name"
            placeholder="e.g. Yarn Dyeing Charges"
            required
            value={name}
            onChange={(e) => setName(e.currentTarget.value)}
          />
          <Select
            label="Account Type"
            required
            data={[
              { value: 'ASSET', label: 'Asset (Debit normal)' },
              { value: 'LIABILITY', label: 'Liability (Credit normal)' },
              { value: 'EQUITY', label: 'Equity (Credit normal)' },
              { value: 'INCOME', label: 'Income (Credit normal)' },
              { value: 'EXPENSE', label: 'Expense (Debit normal)' },
            ]}
            value={accountType}
            onChange={(val) => setAccountType(val || 'EXPENSE')}
          />
          <TextInput
            label="Report Group"
            placeholder="e.g. DIRECT_EXPENSES"
            value={reportGroup}
            onChange={(e) => setReportGroup(e.currentTarget.value)}
          />
          <Group justify="flex-end" mt="md">
            <Button variant="default" onClick={closeAddModal}>
              Cancel
            </Button>
            <Button
              color="indigo"
              onClick={() => createMutation.mutate()}
              loading={createMutation.isPending}
              disabled={!code.trim() || !name.trim()}
            >
              Create Account
            </Button>
          </Group>
        </Stack>
      </Modal>

      {/* Ledger Integrity Modal */}
      <Modal opened={integrityModalOpened} onClose={closeIntegrityModal} title="System Ledger Reconciliation" size="md">
        <Stack gap="sm">
          {integrityChecking ? (
            <Text ta="center" py="md">
              Running verification checks against all journal entries and control accounts...
            </Text>
          ) : integrityResult ? (
            <>
              {integrityResult.healthy ? (
                <Alert color="teal" icon={<IconCheck size={18} />} title="Ledger is 100% Mathematically Balanced">
                  Total Debits match Total Credits across all {integrityResult.totalEntriesChecked} journal entries.
                  Control accounts reconcile with party subsidiary balances.
                </Alert>
              ) : (
                <Alert color="red" icon={<IconAlertTriangle size={18} />} title="Integrity Discrepancy Detected">
                  The system detected imbalances or missing party links:
                  <ul style={{ marginTop: 8, paddingLeft: 20 }}>
                    {integrityResult.violations.map((v, i) => (
                      <li key={i}>{v}</li>
                    ))}
                  </ul>
                </Alert>
              )}

              <Card withBorder padding="sm" radius="md" bg="gray.0" mt="xs">
                <Stack gap={4} fz="xs">
                  <Group justify="space-between">
                    <Text c="dimmed">Entries Checked:</Text>
                    <Text fw={700}>{integrityResult.totalEntriesChecked}</Text>
                  </Group>
                  <Group justify="space-between">
                    <Text c="dimmed">Total Debits:</Text>
                    <Text fw={700}>{formatCurrency(integrityResult.totalDebits)}</Text>
                  </Group>
                  <Group justify="space-between">
                    <Text c="dimmed">Total Credits:</Text>
                    <Text fw={700}>{formatCurrency(integrityResult.totalCredits)}</Text>
                  </Group>
                  <Group justify="space-between">
                    <Text c="dimmed">Net Difference:</Text>
                    <Text fw={700}>{formatCurrency(integrityResult.netDifference)}</Text>
                  </Group>
                </Stack>
              </Card>
            </>
          ) : (
            <Text>No integrity check data available.</Text>
          )}

          <Group justify="flex-end" mt="md">
            <Button variant="default" onClick={closeIntegrityModal}>
              Close
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Container>
  );
};
