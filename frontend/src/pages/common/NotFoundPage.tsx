import React from 'react';
import { Container, Card, Title, Text, Stack, ThemeIcon, Group, Button } from '@mantine/core';
import { IconError404, IconArrowLeft, IconHome } from '@tabler/icons-react';
import { useNavigate, useLocation } from 'react-router-dom';

export const NotFoundPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <Container size="sm" py={60}>
      <Card shadow="sm" padding="xl" radius="md" withBorder ta="center">
        <Stack align="center" gap="md">
          <ThemeIcon size={72} radius="xl" color="red" variant="light">
            <IconError404 size={42} />
          </ThemeIcon>

          <div>
            <Title order={2} fw={700} c="dark.8">
              Page Not Found
            </Title>
            <Text c="dimmed" size="sm" mt="xs">
              The requested address <Text span fw={600} c="indigo.7">{location.pathname}</Text> does not exist in the system.
            </Text>
          </div>

          <Group justify="center" mt="md" gap="sm">
            <Button
              variant="default"
              leftSection={<IconArrowLeft size={16} />}
              onClick={() => navigate(-1)}
            >
              Go Back
            </Button>
            <Button
              color="indigo"
              leftSection={<IconHome size={16} />}
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
