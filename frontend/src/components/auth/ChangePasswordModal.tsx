import React, { useState } from 'react';
import { Modal, PasswordInput, Button, Stack, Text, Alert } from '@mantine/core';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { IconAlertTriangle, IconLockCheck } from '@tabler/icons-react';
import { useAuth } from '../../context/AuthContext';

const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Current password is required'),
    newPassword: z.string().min(12, 'New password must be at least 12 characters'),
    confirmPassword: z.string().min(1, 'Please confirm your new password'),
  })
  .refine((data) => data.newPassword !== data.currentPassword, {
    message: 'New password must be different from current password',
    path: ['newPassword'],
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  });

type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;

export const ChangePasswordModal: React.FC = () => {
  const { user, changePassword } = useAuth();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  if (!user || !user.mustChangePassword) {
    return null;
  }

  const onSubmit = async (values: ChangePasswordFormValues) => {
    setIsSubmitting(true);
    setErrorMessage(null);
    try {
      await changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      reset();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage('Failed to change password. Please verify current password.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      opened={user.mustChangePassword}
      onClose={() => {}}
      closeOnClickOutside={false}
      closeOnEscape={false}
      withCloseButton={false}
      title={
        <Text fw={700} size="lg" c="dark.8">
          Password Change Required
        </Text>
      }
      centered
      radius="md"
      padding="lg"
      shadow="xl"
    >
      <form onSubmit={handleSubmit(onSubmit)}>
        <Stack gap="md">
          <Text size="sm" c="dimmed">
            As a security measure, the initial owner bootstrap password must be replaced with a
            private master password (minimum 12 characters) before accessing system data.
          </Text>

          {errorMessage && (
            <Alert icon={<IconAlertTriangle size={16} />} title="Error" color="red" radius="sm">
              {errorMessage}
            </Alert>
          )}

          <PasswordInput
            label="Current Password"
            placeholder="Enter current password"
            required
            error={errors.currentPassword?.message}
            {...register('currentPassword')}
          />

          <PasswordInput
            label="New Password"
            placeholder="Minimum 12 characters"
            description="Must contain at least 12 characters"
            required
            error={errors.newPassword?.message}
            {...register('newPassword')}
          />

          <PasswordInput
            label="Confirm New Password"
            placeholder="Re-enter new password"
            required
            error={errors.confirmPassword?.message}
            {...register('confirmPassword')}
          />

          <Button
            type="submit"
            fullWidth
            color="indigo"
            loading={isSubmitting}
            leftSection={<IconLockCheck size={18} />}
            mt="sm"
          >
            Update Master Password
          </Button>
        </Stack>
      </form>
    </Modal>
  );
};
