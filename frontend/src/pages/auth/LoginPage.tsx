import React, { useState } from 'react';
import { useNavigate, Navigate, useSearchParams } from 'react-router-dom';
import {
  Container,
  Paper,
  Title,
  Text,
  TextInput,
  PasswordInput,
  Button,
  Alert,
  Stack,
  Group,
  ThemeIcon,
  Badge,
} from '@mantine/core';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  IconLock,
  IconUser,
  IconAlertCircle,
  IconBuildingStore,
  IconShieldLock,
  IconInfoCircle,
} from '@tabler/icons-react';
import { useAuth } from '../../context/AuthContext';

const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export const LoginPage: React.FC = () => {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const isExpired = searchParams.get('expired') === 'true';
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: '',
      password: '',
    },
  });

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  const onSubmit = async (values: LoginFormValues) => {
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await login(values);
      navigate('/', { replace: true });
    } catch (err: unknown) {
      if (err instanceof Error) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage('Invalid username or password.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Container size={440} my={50} px="md">
      <Stack gap="xl">
        <Group justify="center" gap="sm">
          <ThemeIcon size={48} radius="xl" color="indigo" variant="light">
            <IconBuildingStore size={28} />
          </ThemeIcon>
          <div>
            <Title order={2} ta="center" fw={800} c="indigo.8">
              TEX_ARUN
            </Title>
            <Text c="dimmed" size="xs" ta="center" fw={500}>
              Textile Accounts Management System
            </Text>
          </div>
        </Group>

        <Paper withBorder shadow="md" p={30} radius="md">
          <form onSubmit={handleSubmit(onSubmit)}>
            <Stack gap="md">
              <Group justify="space-between">
                <Text fw={600} size="md" c="dark.7">
                  Owner Sign In
                </Text>
                <Badge color="indigo" variant="light" size="sm">
                  Single Owner Mode
                </Badge>
              </Group>

              {isExpired && !errorMessage && (
                <Alert
                  icon={<IconInfoCircle size={16} />}
                  color="yellow"
                  radius="sm"
                  title="Session Expired"
                  role="alert"
                >
                  Your previous session timed out. Please sign in again.
                </Alert>
              )}

              {errorMessage && (
                <Alert
                  icon={<IconAlertCircle size={16} />}
                  color="red"
                  radius="sm"
                  title="Authentication Failed"
                  role="alert"
                >
                  {errorMessage}
                </Alert>
              )}

              <TextInput
                label="Username"
                placeholder="Enter owner username"
                leftSection={<IconUser size={16} />}
                required
                error={errors.username?.message}
                {...register('username')}
              />

              <PasswordInput
                label="Password"
                placeholder="Enter password"
                leftSection={<IconLock size={16} />}
                required
                error={errors.password?.message}
                {...register('password')}
              />

              <Button
                type="submit"
                fullWidth
                color="indigo"
                loading={isSubmitting}
                leftSection={<IconShieldLock size={18} />}
                mt="sm"
              >
                Sign In
              </Button>
            </Stack>
          </form>
        </Paper>

        <Group justify="center" gap="xs">
          <Badge variant="outline" color="gray" size="xs">
            Double-Entry Ledger
          </Badge>
          <Badge variant="outline" color="gray" size="xs">
            GST Compliant
          </Badge>
          <Badge variant="outline" color="gray" size="xs">
            Spring Session JDBC
          </Badge>
        </Group>
      </Stack>
    </Container>
  );
};
