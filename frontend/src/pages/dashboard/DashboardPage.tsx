import React from 'react';
import {
  Container,
  Title,
  Text,
  SimpleGrid,
  Card,
  Group,
  ThemeIcon,
  Badge,
  Stack,
  Button,
  Table,
  Skeleton,
  Divider,
} from '@mantine/core';
import {
  IconReceipt,
  IconShoppingCart,
  IconCash,
  IconBuildingBank,
  IconArrowUpRight,
  IconArrowDownLeft,
  IconPlus,
  IconRefresh,
  IconReportMoney,
} from '@tabler/icons-react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { textileApi, DashboardData } from '../../api/textileApi';
import { formatCurrency, formatDate } from '../../utils/formatters';

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();

  const { data, isLoading, refetch, isFetching } = useQuery<DashboardData>({
    queryKey: ['dashboard'],
    queryFn: textileApi.getDashboard,
    staleTime: 30000,
  });

  const kpis = data?.kpis;

  return (
    <Container size="xl" py="md">
      <Stack gap="lg">
        {/* Header bar */}
        <Group justify="space-between" align="center">
          <div>
            <Title order={2} fw={800} c="dark.8">
              Executive Dashboard
            </Title>
            <Text size="sm" c="dimmed">
              Real-time financial status derived directly from immutable double-entry ledger
            </Text>
          </div>
          <Group gap="xs">
            <Button
              variant="default"
              size="xs"
              leftSection={<IconRefresh size={14} className={isFetching ? 'animate-spin' : ''} />}
              onClick={() => refetch()}
            >
              Refresh
            </Button>
            <Badge size="md" color="indigo" variant="light">
              FY 2026-27
            </Badge>
          </Group>
        </Group>

        {/* 6 Top KPIs */}
        <SimpleGrid cols={{ base: 1, sm: 2, md: 3, lg: 6 }} spacing="sm">
          {/* Today's Sales */}
          <Card shadow="xs" padding="sm" radius="md" withBorder bg="white">
            <Group justify="space-between" mb={4}>
              <Text size="xs" fw={700} c="dimmed" tt="uppercase">
                Today&apos;s Sales
              </Text>
              <ThemeIcon color="indigo" variant="light" size="sm" radius="md">
                <IconReceipt size={14} />
              </ThemeIcon>
            </Group>
            {isLoading ? (
              <Skeleton height={28} width="80%" mt={4} />
            ) : (
              <Text fw={800} size="lg" c="indigo.8">
                {formatCurrency(kpis?.todaySales)}
              </Text>
            )}
            <Text size="xs" c="dimmed" mt={2}>
              Tax Invoices
            </Text>
          </Card>

          {/* Month's Sales */}
          <Card shadow="xs" padding="sm" radius="md" withBorder bg="white">
            <Group justify="space-between" mb={4}>
              <Text size="xs" fw={700} c="dimmed" tt="uppercase">
                This Month Sales
              </Text>
              <ThemeIcon color="teal" variant="light" size="sm" radius="md">
                <IconReceipt size={14} />
              </ThemeIcon>
            </Group>
            {isLoading ? (
              <Skeleton height={28} width="80%" mt={4} />
            ) : (
              <Text fw={800} size="lg" c="teal.8">
                {formatCurrency(kpis?.thisMonthSales)}
              </Text>
            )}
            <Text size="xs" c="dimmed" mt={2}>
              Month to Date
            </Text>
          </Card>

          {/* Customer Outstanding (AR) */}
          <Card shadow="xs" padding="sm" radius="md" withBorder bg="white">
            <Group justify="space-between" mb={4}>
              <Text size="xs" fw={700} c="dimmed" tt="uppercase">
                Receivables (AR)
              </Text>
              <ThemeIcon color="orange" variant="light" size="sm" radius="md">
                <IconArrowDownLeft size={14} />
              </ThemeIcon>
            </Group>
            {isLoading ? (
              <Skeleton height={28} width="80%" mt={4} />
            ) : (
              <Text fw={800} size="lg" c="orange.8">
                {formatCurrency(kpis?.customerOutstanding)}
              </Text>
            )}
            <Text size="xs" c="dimmed" mt={2}>
              Due from Customers
            </Text>
          </Card>

          {/* Supplier Outstanding (AP) */}
          <Card shadow="xs" padding="sm" radius="md" withBorder bg="white">
            <Group justify="space-between" mb={4}>
              <Text size="xs" fw={700} c="dimmed" tt="uppercase">
                Payables (AP)
              </Text>
              <ThemeIcon color="red" variant="light" size="sm" radius="md">
                <IconArrowUpRight size={14} />
              </ThemeIcon>
            </Group>
            {isLoading ? (
              <Skeleton height={28} width="80%" mt={4} />
            ) : (
              <Text fw={800} size="lg" c="red.8">
                {formatCurrency(kpis?.supplierOutstanding)}
              </Text>
            )}
            <Text size="xs" c="dimmed" mt={2}>
              Due to Suppliers
            </Text>
          </Card>

          {/* Cash Balance */}
          <Card shadow="xs" padding="sm" radius="md" withBorder bg="white">
            <Group justify="space-between" mb={4}>
              <Text size="xs" fw={700} c="dimmed" tt="uppercase">
                Cash in Hand
              </Text>
              <ThemeIcon color="emerald" variant="light" size="sm" radius="md">
                <IconCash size={14} />
              </ThemeIcon>
            </Group>
            {isLoading ? (
              <Skeleton height={28} width="80%" mt={4} />
            ) : (
              <Text fw={800} size="lg" c="green.8">
                {formatCurrency(kpis?.cashBalance)}
              </Text>
            )}
            <Text size="xs" c="dimmed" mt={2}>
              Counter Register
            </Text>
          </Card>

          {/* Bank Balance */}
          <Card shadow="xs" padding="sm" radius="md" withBorder bg="white">
            <Group justify="space-between" mb={4}>
              <Text size="xs" fw={700} c="dimmed" tt="uppercase">
                Bank Balance
              </Text>
              <ThemeIcon color="blue" variant="light" size="sm" radius="md">
                <IconBuildingBank size={14} />
              </ThemeIcon>
            </Group>
            {isLoading ? (
              <Skeleton height={28} width="80%" mt={4} />
            ) : (
              <Text fw={800} size="lg" c="blue.8">
                {formatCurrency(kpis?.bankBalance)}
              </Text>
            )}
            <Text size="xs" c="dimmed" mt={2}>
              Active Accounts
            </Text>
          </Card>
        </SimpleGrid>

        {/* Quick Operations Bar */}
        <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
          <Group justify="space-between" align="center">
            <div>
              <Text fw={700} size="sm" c="dark.8">
                Quick Workflows
              </Text>
              <Text size="xs" c="dimmed">
                Perform transactions with automatic real-time double-entry posting
              </Text>
            </div>
            <Group gap="xs">
              <Button
                variant="filled"
                color="indigo"
                size="xs"
                leftSection={<IconPlus size={14} />}
                onClick={() => navigate('/sales?create=true')}
              >
                + Sales Invoice
              </Button>
              <Button
                variant="filled"
                color="teal"
                size="xs"
                leftSection={<IconPlus size={14} />}
                onClick={() => navigate('/purchases?create=true')}
              >
                + Purchase Bill
              </Button>
              <Button
                variant="light"
                color="cyan"
                size="xs"
                leftSection={<IconCash size={14} />}
                onClick={() => navigate('/payments?tab=receipts&create=true')}
              >
                + Customer Receipt
              </Button>
              <Button
                variant="light"
                color="orange"
                size="xs"
                leftSection={<IconCash size={14} />}
                onClick={() => navigate('/payments?tab=payments&create=true')}
              >
                + Supplier Payment
              </Button>
              <Button
                variant="light"
                color="pink"
                size="xs"
                leftSection={<IconReportMoney size={14} />}
                onClick={() => navigate('/expenses?create=true')}
              >
                + Expense
              </Button>
            </Group>
          </Group>
        </Card>

        {/* Recent Invoices & Recent Purchases */}
        <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
          {/* Recent Sales Invoices */}
          <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
            <Group justify="space-between" mb="sm">
              <Group gap="xs">
                <ThemeIcon color="indigo" variant="light" size="sm" radius="md">
                  <IconReceipt size={14} />
                </ThemeIcon>
                <Text fw={700} size="sm">
                  Recent Sales Invoices
                </Text>
              </Group>
              <Button variant="subtle" color="indigo" size="xs" onClick={() => navigate('/sales')}>
                View All
              </Button>
            </Group>
            <Divider mb="xs" />

            {isLoading ? (
              <Stack gap="xs">
                <Skeleton height={28} />
                <Skeleton height={28} />
                <Skeleton height={28} />
              </Stack>
            ) : !data?.recentSales || data.recentSales.length === 0 ? (
              <Text size="sm" c="dimmed" ta="center" py="lg">
                No sales invoices created yet. Click &quot;+ Sales Invoice&quot; to begin.
              </Text>
            ) : (
              <Table.ScrollContainer minWidth={350}>
                <Table striped highlightOnHover verticalSpacing="xs" fz="xs">
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Invoice #</Table.Th>
                      <Table.Th>Customer</Table.Th>
                      <Table.Th>Date</Table.Th>
                      <Table.Th ta="right">Amount</Table.Th>
                      <Table.Th>Status</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {data.recentSales.map((inv) => (
                      <Table.Tr key={inv.id}>
                        <Table.Td fw={600}>{inv.invoiceNo || 'DRAFT'}</Table.Td>
                        <Table.Td>{inv.customerName}</Table.Td>
                        <Table.Td>{formatDate(inv.invoiceDate)}</Table.Td>
                        <Table.Td ta="right" fw={700}>
                          {formatCurrency(inv.grandTotal)}
                        </Table.Td>
                        <Table.Td>
                          <Badge
                            size="xs"
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
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>
            )}
          </Card>

          {/* Recent Purchases */}
          <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
            <Group justify="space-between" mb="sm">
              <Group gap="xs">
                <ThemeIcon color="teal" variant="light" size="sm" radius="md">
                  <IconShoppingCart size={14} />
                </ThemeIcon>
                <Text fw={700} size="sm">
                  Recent Purchase Bills
                </Text>
              </Group>
              <Button variant="subtle" color="teal" size="xs" onClick={() => navigate('/purchases')}>
                View All
              </Button>
            </Group>
            <Divider mb="xs" />

            {isLoading ? (
              <Stack gap="xs">
                <Skeleton height={28} />
                <Skeleton height={28} />
                <Skeleton height={28} />
              </Stack>
            ) : !data?.recentPurchases || data.recentPurchases.length === 0 ? (
              <Text size="sm" c="dimmed" ta="center" py="lg">
                No purchase bills recorded yet. Click &quot;+ Purchase Bill&quot; to begin.
              </Text>
            ) : (
              <Table.ScrollContainer minWidth={350}>
                <Table striped highlightOnHover verticalSpacing="xs" fz="xs">
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>Voucher #</Table.Th>
                      <Table.Th>Supplier</Table.Th>
                      <Table.Th>Date</Table.Th>
                      <Table.Th ta="right">Amount</Table.Th>
                      <Table.Th>Status</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {data.recentPurchases.map((bill) => (
                      <Table.Tr key={bill.id}>
                        <Table.Td fw={600}>{bill.voucherNo || 'DRAFT'}</Table.Td>
                        <Table.Td>{bill.supplierName}</Table.Td>
                        <Table.Td>{formatDate(bill.purchaseDate)}</Table.Td>
                        <Table.Td ta="right" fw={700}>
                          {formatCurrency(bill.grandTotal)}
                        </Table.Td>
                        <Table.Td>
                          <Badge
                            size="xs"
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
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>
            )}
          </Card>
        </SimpleGrid>
      </Stack>
    </Container>
  );
};
