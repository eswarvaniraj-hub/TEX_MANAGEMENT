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
  Select,
  Stack,
  SimpleGrid,
  Alert,
  Modal,
  rem,
} from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import {
  IconShieldLock,
  IconRefresh,
  IconSearch,
  IconAlertCircle,
  IconEye,
  IconActivity,
  IconUserCheck,
} from '@tabler/icons-react';
import { textileApi, AuditEventItem } from '../../api/textileApi';

export const AuditLogPage: React.FC = () => {
  const [search, setSearch] = useState<string>('');
  const [selectedAction, setSelectedAction] = useState<string | null>(null);
  const [selectedEvent, setSelectedEvent] = useState<AuditEventItem | null>(null);

  const {
    data: events = [],
    isLoading,
    isError,
    refetch,
    isRefetching,
  } = useQuery({
    queryKey: ['auditEvents'],
    queryFn: () => textileApi.getAuditEvents(100),
  });

  const filteredEvents = events.filter((ev) => {
    const matchesSearch =
      !search ||
      ev.actorUsername.toLowerCase().includes(search.toLowerCase()) ||
      ev.summary.toLowerCase().includes(search.toLowerCase()) ||
      (ev.entityId && ev.entityId.toLowerCase().includes(search.toLowerCase())) ||
      (ev.ipAddress && ev.ipAddress.includes(search));

    const matchesAction = !selectedAction || ev.action.toLowerCase().includes(selectedAction.toLowerCase());

    return matchesSearch && matchesAction;
  });

  const getActionColor = (action: string) => {
    const act = action.toUpperCase();
    if (act.includes('FAIL') || act.includes('LOCKOUT') || act.includes('REVERSE')) return 'red';
    if (act.includes('SUCCESS') || act.includes('POST') || act.includes('LOGIN')) return 'green';
    if (act.includes('CREATE') || act.includes('UPDATE')) return 'blue';
    return 'gray';
  };

  const loginEventsCount = events.filter((e) => e.action.includes('LOGIN') || e.action.includes('AUTH')).length;
  const transactionEventsCount = events.filter((e) =>
    ['SALES', 'PURCHASE', 'PAYMENT', 'EXPENSE', 'BANKING'].some((k) => e.entityType?.includes(k))
  ).length;

  return (
    <Container fluid px="md" py="lg">
      <Stack gap="lg">
        {/* Header */}
        <Group justify="space-between" align="center">
          <div>
            <Group gap="xs">
              <IconShieldLock size={28} color="#2b8a3e" />
              <Title order={2} fw={800}>
                Security &amp; Transaction Audit Log
              </Title>
            </Group>
            <Text c="dimmed" size="sm">
              Append-only audit trail recording user logins, financial voucher postings, and configuration updates.
            </Text>
          </div>
          <Button
            leftSection={<IconRefresh size={16} />}
            variant="light"
            color="indigo"
            onClick={() => refetch()}
            loading={isRefetching}
          >
            Refresh Log
          </Button>
        </Group>

        {/* Metrics Cards */}
        <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="md">
          <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
            <Group justify="space-between">
              <div>
                <Text size="xs" c="dimmed" fw={700} tt="uppercase">
                  Total Logged Events
                </Text>
                <Title order={3} fw={800} mt={4}>
                  {events.length}
                </Title>
              </div>
              <IconActivity size={32} color="#4c6ef5" />
            </Group>
          </Card>

          <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
            <Group justify="space-between">
              <div>
                <Text size="xs" c="dimmed" fw={700} tt="uppercase">
                  Auth &amp; Security Audits
                </Text>
                <Title order={3} fw={800} mt={4} c="teal.7">
                  {loginEventsCount}
                </Title>
              </div>
              <IconUserCheck size={32} color="#12b886" />
            </Group>
          </Card>

          <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
            <Group justify="space-between">
              <div>
                <Text size="xs" c="dimmed" fw={700} tt="uppercase">
                  Accounting Operations
                </Text>
                <Title order={3} fw={800} mt={4} c="indigo.7">
                  {transactionEventsCount}
                </Title>
              </div>
              <IconShieldLock size={32} color="#7950f2" />
            </Group>
          </Card>
        </SimpleGrid>

        {isError && (
          <Alert icon={<IconAlertCircle size={16} />} title="Error" color="red">
            Failed to load audit logs. Please try again.
          </Alert>
        )}

        {/* Filters */}
        <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
          <Group justify="space-between" align="center">
            <Group>
              <TextInput
                placeholder="Search actor, summary, IP or ref..."
                leftSection={<IconSearch size={16} />}
                value={search}
                onChange={(e) => setSearch(e.currentTarget.value)}
                style={{ width: 280 }}
              />
              <Select
                placeholder="Filter by action category"
                clearable
                data={[
                  { value: 'LOGIN', label: 'Login & Session' },
                  { value: 'PASSWORD', label: 'Password Changes' },
                  { value: 'CREATE', label: 'Record Created' },
                  { value: 'POST', label: 'Voucher Posted' },
                  { value: 'REVERSE', label: 'Voucher Reversed' },
                ]}
                value={selectedAction}
                onChange={setSelectedAction}
                style={{ width: 220 }}
              />
            </Group>
            <Badge variant="outline" color="gray">
              Showing {filteredEvents.length} events
            </Badge>
          </Group>
        </Card>

        {/* Audit Log Table */}
        <Card shadow="xs" padding={0} radius="md" withBorder bg="white" style={{ overflow: 'hidden' }}>
          <Table striped highlightOnHover>
            <Table.Thead bg="gray.1">
              <Table.Tr>
                <Table.Th style={{ width: 180 }}>Timestamp</Table.Th>
                <Table.Th style={{ width: 130 }}>Actor</Table.Th>
                <Table.Th style={{ width: 140 }}>Action</Table.Th>
                <Table.Th style={{ width: 140 }}>Entity</Table.Th>
                <Table.Th>Summary</Table.Th>
                <Table.Th style={{ width: 130 }}>IP Address</Table.Th>
                <Table.Th style={{ width: 80, textAlign: 'center' }}>Details</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {isLoading ? (
                <Table.Tr>
                  <Table.Td colSpan={7} style={{ textAlign: 'center', padding: rem(30) }}>
                    <Text c="dimmed">Loading audit events...</Text>
                  </Table.Td>
                </Table.Tr>
              ) : filteredEvents.length === 0 ? (
                <Table.Tr>
                  <Table.Td colSpan={7} style={{ textAlign: 'center', padding: rem(30) }}>
                    <Text c="dimmed">No audit events match your filters.</Text>
                  </Table.Td>
                </Table.Tr>
              ) : (
                filteredEvents.map((ev) => (
                  <Table.Tr key={ev.id}>
                    <Table.Td style={{ fontSize: '0.82rem', fontFamily: 'monospace' }}>
                      {ev.occurredAt ? new Date(ev.occurredAt).toLocaleString('en-IN') : '-'}
                    </Table.Td>
                    <Table.Td>
                      <Badge variant="light" color="indigo" size="sm">
                        {ev.actorUsername}
                      </Badge>
                    </Table.Td>
                    <Table.Td>
                      <Badge variant="filled" color={getActionColor(ev.action)} size="sm">
                        {ev.action}
                      </Badge>
                    </Table.Td>
                    <Table.Td>
                      <Text size="xs" fw={600}>
                        {ev.entityType}
                      </Text>
                      {ev.entityId && (
                        <Text size="xs" c="dimmed">
                          ID: {ev.entityId}
                        </Text>
                      )}
                    </Table.Td>
                    <Table.Td>
                      <Text size="sm">{ev.summary}</Text>
                    </Table.Td>
                    <Table.Td style={{ fontSize: '0.82rem', fontFamily: 'monospace', color: '#666' }}>
                      {ev.ipAddress || 'internal'}
                    </Table.Td>
                    <Table.Td style={{ textAlign: 'center' }}>
                      <Button
                        variant="subtle"
                        size="xs"
                        p={4}
                        onClick={() => setSelectedEvent(ev)}
                      >
                        <IconEye size={16} />
                      </Button>
                    </Table.Td>
                  </Table.Tr>
                ))
              )}
            </Table.Tbody>
          </Table>
        </Card>

        {/* Modal for viewing detailed event */}
        <Modal
          opened={!!selectedEvent}
          onClose={() => setSelectedEvent(null)}
          title={
            <Group gap="xs">
              <IconShieldLock size={20} color="#2b8a3e" />
              <Text fw={700}>Audit Event #{selectedEvent?.id}</Text>
            </Group>
          }
          size="md"
        >
          {selectedEvent && (
            <Stack gap="sm">
              <SimpleGrid cols={2} spacing="xs">
                <div>
                  <Text size="xs" c="dimmed">
                    Timestamp:
                  </Text>
                  <Text size="sm" fw={600}>
                    {new Date(selectedEvent.occurredAt).toLocaleString('en-IN')}
                  </Text>
                </div>
                <div>
                  <Text size="xs" c="dimmed">
                    Actor:
                  </Text>
                  <Text size="sm" fw={600}>
                    {selectedEvent.actorUsername} (User #{selectedEvent.actorUserId || 'N/A'})
                  </Text>
                </div>
                <div>
                  <Text size="xs" c="dimmed">
                    Action:
                  </Text>
                  <Badge color={getActionColor(selectedEvent.action)}>
                    {selectedEvent.action}
                  </Badge>
                </div>
                <div>
                  <Text size="xs" c="dimmed">
                    IP Address:
                  </Text>
                  <Text size="sm" fw={600}>
                    {selectedEvent.ipAddress || 'Internal'}
                  </Text>
                </div>
                <div>
                  <Text size="xs" c="dimmed">
                    Entity Type:
                  </Text>
                  <Text size="sm" fw={600}>
                    {selectedEvent.entityType}
                  </Text>
                </div>
                <div>
                  <Text size="xs" c="dimmed">
                    Entity ID:
                  </Text>
                  <Text size="sm" fw={600}>
                    {selectedEvent.entityId || 'N/A'}
                  </Text>
                </div>
              </SimpleGrid>

              <div>
                <Text size="xs" c="dimmed" mb={4}>
                  Summary:
                </Text>
                <Card bg="gray.1" padding="xs" radius="sm">
                  <Text size="sm">{selectedEvent.summary}</Text>
                </Card>
              </div>

              <Button fullWidth variant="light" color="gray" onClick={() => setSelectedEvent(null)} mt="md">
                Close
              </Button>
            </Stack>
          )}
        </Modal>
      </Stack>
    </Container>
  );
};
