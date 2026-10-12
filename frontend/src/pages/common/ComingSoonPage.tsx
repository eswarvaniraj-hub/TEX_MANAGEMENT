import React from 'react';
import {
  Container,
  Card,
  Title,
  Text,
  Stack,
  Badge,
  ThemeIcon,
  Group,
  Button,
} from '@mantine/core';
import { IconClock, IconArrowLeft } from '@tabler/icons-react';
import { useNavigate } from 'react-router-dom';

interface ComingSoonPageProps {
  moduleName: string;
  phase: string;
  description?: string;
}

export const ComingSoonPage: React.FC<ComingSoonPageProps> = ({
  moduleName,
  phase,
  description = 'This module will be unlocked in the upcoming development phase in accordance with docs/IMPLEMENTATION_PLAN.md.',
}) => {
  const navigate = useNavigate();

  return (
    <Container size="sm" py={50}>
      <Card shadow="sm" padding="xl" radius="md" withBorder ta="center">
        <Stack align="center" gap="md">
          <ThemeIcon size={64} radius="xl" color="indigo" variant="light">
            <IconClock size={36} />
          </ThemeIcon>

          <div>
            <Badge color="indigo" variant="light" size="md" mb="xs">
              {phase}
            </Badge>
            <Title order={2} fw={700} c="dark.8">
              {moduleName}
            </Title>
            <Text c="dimmed" size="sm" mt="xs" maw={400} mx="auto">
              {description}
            </Text>
          </div>

          <Group justify="center" mt="md">
            <Button
              variant="outline"
              color="indigo"
              leftSection={<IconArrowLeft size={16} />}
              onClick={() => navigate('/')}
            >
              Back to Dashboard
            </Button>
          </Group>
        </Stack>
      </Card>
    </Container>
  );
};
