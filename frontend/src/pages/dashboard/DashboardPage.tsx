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
} from '@mantine/core';
import {
  IconShieldCheck,
  IconCalendar,
  IconDatabase,
  IconReceipt,
  IconShoppingCart,
  IconUsers,
  IconArrowRight,
} from '@tabler/icons-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  return (
    <Container size="xl" py="md">
      <Stack gap="lg">
        <div>
          <Group justify="space-between" align="flex-end">
            <div>
              <Title order={2} fw={700} c="dark.8">
                Welcome back, {user?.username}
              </Title>
              <Text size="sm" c="dimmed">
                Financial overview and management console for Textile Accounts
              </Text>
            </div>
            <Badge size="lg" color="indigo" variant="light">
              Phase 2: Authentication &amp; Layout Active
            </Badge>
          </Group>
        </div>

        <SimpleGrid cols={{ base: 1, sm: 2, md: 3 }} spacing="md">
          <Card shadow="xs" padding="lg" radius="md" withBorder>
            <Group justify="space-between" mb="xs">
              <Text size="xs" fw={700} c="dimmed" tt="uppercase">
                Authentication &amp; Session
              </Text>
              <ThemeIcon color="teal" variant="light" size="md">
                <IconShieldCheck size={18} />
              </ThemeIcon>
            </Group>
            <Text fw={700} size="xl" c="dark.8">
              Secured (Single Owner)
            </Text>
            <Text size="xs" c="dimmed" mt={4}>
              Spring Session JDBC • CSRF Protected • 15m Lockout
            </Text>
          </Card>

          <Card shadow="xs" padding="lg" radius="md" withBorder>
            <Group justify="space-between" mb="xs">
              <Text size="xs" fw={700} c="dimmed" tt="uppercase">
                Active Financial Year
              </Text>
              <ThemeIcon color="indigo" variant="light" size="md">
                <IconCalendar size={18} />
              </ThemeIcon>
            </Group>
            <Text fw={700} size="xl" c="dark.8">
              FY 2026 - 2027
            </Text>
            <Text size="xs" c="dimmed" mt={4}>
              Starts 01 Apr 2026 • Ends 31 Mar 2027
            </Text>
          </Card>

          <Card shadow="xs" padding="lg" radius="md" withBorder>
            <Group justify="space-between" mb="xs">
              <Text size="xs" fw={700} c="dimmed" tt="uppercase">
                Database Engine
              </Text>
              <ThemeIcon color="blue" variant="light" size="md">
                <IconDatabase size={18} />
              </ThemeIcon>
            </Group>
            <Text fw={700} size="xl" c="dark.8">
              PostgreSQL
            </Text>
            <Text size="xs" c="dimmed" mt={4}>
              Flyway V1 Schema Validated • Numeric Precision Enforced
            </Text>
          </Card>
        </SimpleGrid>

        <Card shadow="xs" padding="lg" radius="md" withBorder>
          <Title order={4} mb="md" c="dark.8">
            Quick Navigation &amp; Workflows
          </Title>
          <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="md">
            <Card
              padding="md"
              radius="sm"
              style={{ backgroundColor: '#f8fafc', border: '1px solid #e2e8f0' }}
            >
              <ThemeIcon color="indigo" size="lg" radius="md" mb="sm">
                <IconReceipt size={22} />
              </ThemeIcon>
              <Text fw={600} size="sm">
                Sales Invoices
              </Text>
              <Text size="xs" c="dimmed" mb="md">
                Generate GST-compliant tax invoices with automatic CGST, SGST, IGST calculations.
              </Text>
              <Button
                variant="light"
                color="indigo"
                size="xs"
                rightSection={<IconArrowRight size={14} />}
                onClick={() => navigate('/sales')}
              >
                Go to Sales
              </Button>
            </Card>

            <Card
              padding="md"
              radius="sm"
              style={{ backgroundColor: '#f8fafc', border: '1px solid #e2e8f0' }}
            >
              <ThemeIcon color="teal" size="lg" radius="md" mb="sm">
                <IconShoppingCart size={22} />
              </ThemeIcon>
              <Text fw={600} size="sm">
                Purchase Bills
              </Text>
              <Text size="xs" c="dimmed" mb="md">
                Record supplier yarn and fabric purchases with transport and tax tracking.
              </Text>
              <Button
                variant="light"
                color="teal"
                size="xs"
                rightSection={<IconArrowRight size={14} />}
                onClick={() => navigate('/purchases')}
              >
                Go to Purchases
              </Button>
            </Card>

            <Card
              padding="md"
              radius="sm"
              style={{ backgroundColor: '#f8fafc', border: '1px solid #e2e8f0' }}
            >
              <ThemeIcon color="grape" size="lg" radius="md" mb="sm">
                <IconUsers size={22} />
              </ThemeIcon>
              <Text fw={600} size="sm">
                Parties &amp; Master Data
              </Text>
              <Text size="xs" c="dimmed" mb="md">
                Manage buyers, sellers, weavers, and textile product HSN code definitions.
              </Text>
              <Button
                variant="light"
                color="grape"
                size="xs"
                rightSection={<IconArrowRight size={14} />}
                onClick={() => navigate('/parties')}
              >
                Go to Parties
              </Button>
            </Card>
          </SimpleGrid>
        </Card>
      </Stack>
    </Container>
  );
};
