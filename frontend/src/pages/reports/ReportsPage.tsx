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
  TextInput,
  Stack,
  Tabs,
} from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import {
  IconScale,
  IconCalendar,
  IconPrinter,
  IconCheck,
  IconAlertTriangle,
} from '@tabler/icons-react';
import { textileApi, TrialBalanceReport, DayBookLine } from '../../api/textileApi';
import { formatCurrency, formatDate } from '../../utils/formatters';

export const ReportsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('trial-balance');
  const [tbDate, setTbDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [dayBookDate, setDayBookDate] = useState<string>(new Date().toISOString().split('T')[0]);

  const { data: tbData, isLoading: tbLoading } = useQuery<TrialBalanceReport>({
    queryKey: ['trialBalance', tbDate],
    queryFn: () => textileApi.getTrialBalance(tbDate),
  });

  const { data: dayBookLines, isLoading: dbLoading } = useQuery<DayBookLine[]>({
    queryKey: ['dayBook', dayBookDate],
    queryFn: () => textileApi.getDayBook(dayBookDate),
  });

  return (
    <Container size="xl" py="md">
      <Stack gap="lg">
        {/* Header */}
        <Group justify="space-between" align="center">
          <div>
            <Title order={2} fw={800} c="dark.8">
              Financial Reports &amp; Registers
            </Title>
            <Text size="sm" c="dimmed">
              Trial Balance, Day Book, and General Ledger accounts statements
            </Text>
          </div>
          <Button variant="default" size="sm" leftSection={<IconPrinter size={16} />} onClick={() => window.print()}>
            Print / Export
          </Button>
        </Group>

        {/* Tab selector */}
        <Tabs value={activeTab} onChange={(val) => setActiveTab(val || 'trial-balance')}>
          <Tabs.List>
            <Tabs.Tab value="trial-balance" leftSection={<IconScale size={16} />}>
              Trial Balance
            </Tabs.Tab>
            <Tabs.Tab value="day-book" leftSection={<IconCalendar size={16} />}>
              Day Book (Cash &amp; JV Register)
            </Tabs.Tab>
          </Tabs.List>
        </Tabs>

        {/* Trial Balance Tab */}
        {activeTab === 'trial-balance' && (
          <Stack gap="md">
            <Card shadow="xs" padding="sm" radius="md" withBorder bg="white">
              <Group justify="space-between">
                <TextInput
                  label="As of Date"
                  type="date"
                  size="xs"
                  value={tbDate}
                  onChange={(e) => setTbDate(e.currentTarget.value)}
                />
                {tbData && (
                  <Badge
                    size="lg"
                    color={tbData.balanced ? 'teal' : 'red'}
                    leftSection={tbData.balanced ? <IconCheck size={14} /> : <IconAlertTriangle size={14} />}
                  >
                    {tbData.balanced ? 'Trial Balance Balanced' : `Imbalance: ${formatCurrency(tbData.difference)}`}
                  </Badge>
                )}
              </Group>
            </Card>

            <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
              <Table.ScrollContainer minWidth={700}>
                <Table striped highlightOnHover verticalSpacing="sm" fz="sm">
                  <Table.Thead bg="gray.1">
                    <Table.Tr>
                      <Table.Th>Account Code</Table.Th>
                      <Table.Th>Account Name</Table.Th>
                      <Table.Th>Classification</Table.Th>
                      <Table.Th ta="right">Debit (Dr)</Table.Th>
                      <Table.Th ta="right">Credit (Cr)</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {tbLoading ? (
                      <Table.Tr>
                        <Table.Td colSpan={5} ta="center" py="xl">
                          Computing Trial Balance from journal lines...
                        </Table.Td>
                      </Table.Tr>
                    ) : !tbData?.lines || tbData.lines.length === 0 ? (
                      <Table.Tr>
                        <Table.Td colSpan={5} ta="center" py="xl" c="dimmed">
                          No posted ledger entries found up to this date.
                        </Table.Td>
                      </Table.Tr>
                    ) : (
                      tbData.lines.map((l) => (
                        <Table.Tr key={l.accountId}>
                          <Table.Td fw={700} c="indigo.8">
                            {l.code}
                          </Table.Td>
                          <Table.Td fw={600}>{l.name}</Table.Td>
                          <Table.Td>
                            <Badge size="xs" variant="light" color="gray">
                              {l.accountType}
                            </Badge>
                          </Table.Td>
                          <Table.Td ta="right">{Number(l.debit) > 0 ? formatCurrency(l.debit) : '-'}</Table.Td>
                          <Table.Td ta="right">{Number(l.credit) > 0 ? formatCurrency(l.credit) : '-'}</Table.Td>
                        </Table.Tr>
                      ))
                    )}
                  </Table.Tbody>
                  {tbData && tbData.lines.length > 0 && (
                    <Table.Tfoot>
                      <Table.Tr bg="indigo.0">
                        <Table.Th colSpan={3} fw={800}>
                          Total Balance
                        </Table.Th>
                        <Table.Th ta="right" fw={800} c="indigo.9">
                          {formatCurrency(tbData.totalDebits)}
                        </Table.Th>
                        <Table.Th ta="right" fw={800} c="indigo.9">
                          {formatCurrency(tbData.totalCredits)}
                        </Table.Th>
                      </Table.Tr>
                    </Table.Tfoot>
                  )}
                </Table>
              </Table.ScrollContainer>
            </Card>
          </Stack>
        )}

        {/* Day Book Tab */}
        {activeTab === 'day-book' && (
          <Stack gap="md">
            <Card shadow="xs" padding="sm" radius="md" withBorder bg="white">
              <TextInput
                label="Selected Date"
                type="date"
                size="xs"
                value={dayBookDate}
                onChange={(e) => setDayBookDate(e.currentTarget.value)}
                style={{ width: 200 }}
              />
            </Card>

            <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
              <Table.ScrollContainer minWidth={700}>
                <Table striped highlightOnHover verticalSpacing="sm" fz="sm">
                  <Table.Thead bg="gray.1">
                    <Table.Tr>
                      <Table.Th>JV #</Table.Th>
                      <Table.Th>Account</Table.Th>
                      <Table.Th>Party / Tag</Table.Th>
                      <Table.Th>Narration</Table.Th>
                      <Table.Th ta="right">Debit (Dr)</Table.Th>
                      <Table.Th ta="right">Credit (Cr)</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {dbLoading ? (
                      <Table.Tr>
                        <Table.Td colSpan={6} ta="center" py="xl">
                          Loading Day Book transactions...
                        </Table.Td>
                      </Table.Tr>
                    ) : !dayBookLines || dayBookLines.length === 0 ? (
                      <Table.Tr>
                        <Table.Td colSpan={6} ta="center" py="xl" c="dimmed">
                          No transactions recorded on {formatDate(dayBookDate)}.
                        </Table.Td>
                      </Table.Tr>
                    ) : (
                      dayBookLines.map((line, idx) => (
                        <Table.Tr key={idx}>
                          <Table.Td fw={700} c="indigo.8">
                            {line.entryNo}
                          </Table.Td>
                          <Table.Td>
                            {line.accountName} ({line.accountCode})
                          </Table.Td>
                          <Table.Td>{line.partyName || '-'}</Table.Td>
                          <Table.Td>{line.narration}</Table.Td>
                          <Table.Td ta="right">{Number(line.debit) > 0 ? formatCurrency(line.debit) : '-'}</Table.Td>
                          <Table.Td ta="right">{Number(line.credit) > 0 ? formatCurrency(line.credit) : '-'}</Table.Td>
                        </Table.Tr>
                      ))
                    )}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>
            </Card>
          </Stack>
        )}
      </Stack>
    </Container>
  );
};
