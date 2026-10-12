import { useQuery } from '@tanstack/react-query';
import { Container, Card, Text, Badge, Group, Title, Loader, Stack, Alert } from '@mantine/core';
import { IconCheck, IconAlertTriangle, IconServer } from '@tabler/icons-react';

interface HealthResponse {
  status: string;
}

export async function fetchHealth(): Promise<HealthResponse> {
  const response = await fetch('/api/v1/health');
  if (!response.ok) {
    throw new Error(`Health check failed with HTTP ${response.status}`);
  }
  return response.json();
}

export default function App() {
  const { data, error, isLoading } = useQuery<HealthResponse>({
    queryKey: ['systemHealth'],
    queryFn: fetchHealth,
  });

  return (
    <Container size="sm" py="xl">
      <Stack gap="lg">
        <div>
          <Title order={2} c="dark.8">
            Textile Accounts Management
          </Title>
          <Text c="dimmed" size="sm">
            Phase 1: Foundation &amp; Architecture Verification
          </Text>
        </div>

        <Card shadow="sm" padding="lg" radius="md" withBorder>
          <Group justify="space-between" mb="xs">
            <Group gap="xs">
              <IconServer size={22} color="#4f46e5" />
              <Text fw={600} size="md">
                Backend System Status
              </Text>
            </Group>

            {isLoading && <Loader size="xs" color="indigo" />}
            {data && (
              <Badge
                color={data.status === 'UP' ? 'teal' : 'red'}
                variant="light"
                leftSection={<IconCheck size={12} />}
              >
                {data.status}
              </Badge>
            )}
            {error && (
              <Badge color="red" variant="light" leftSection={<IconAlertTriangle size={12} />}>
                UNAVAILABLE
              </Badge>
            )}
          </Group>

          <Text size="sm" c="dimmed" mt="xs">
            Connected to Spring Boot 4.1.1 via Vite dev proxy at{' '}
            <Text span fw={500} c="dark.6">
              /api/v1/health
            </Text>
            .
          </Text>

          {error && (
            <Alert color="red" title="Connection Error" mt="md" radius="sm">
              {error instanceof Error ? error.message : 'Failed to connect to backend service'}
            </Alert>
          )}
        </Card>
      </Stack>
    </Container>
  );
}
