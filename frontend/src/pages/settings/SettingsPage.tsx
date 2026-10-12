import React, { useState, useEffect } from 'react';
import {
  Container,
  Title,
  Text,
  Group,
  Button,
  Card,
  TextInput,
  Select,
  Stack,
  Switch,
  SimpleGrid,
  Divider,
  Alert,
  Badge,
} from '@mantine/core';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  IconBuildingStore,
  IconLock,
  IconCheck,
  IconShieldCheck,
} from '@tabler/icons-react';
import { textileApi, BusinessProfile, GstState } from '../../api/textileApi';
import { formatDate } from '../../utils/formatters';

export const SettingsPage: React.FC = () => {
  const queryClient = useQueryClient();

  const [tradeName, setTradeName] = useState<string>('');
  const [legalName, setLegalName] = useState<string>('');
  const [address, setAddress] = useState<string>('');
  const [stateCode, setStateCode] = useState<string>('33');
  const [gstin, setGstin] = useState<string>('');
  const [pan, setPan] = useState<string>('');
  const [phone, setPhone] = useState<string>('');
  const [email, setEmail] = useState<string>('');
  const [bankDetailsText, setBankDetailsText] = useState<string>('');
  const [invoiceFooter, setInvoiceFooter] = useState<string>('');
  const [roundTotal, setRoundTotal] = useState<boolean>(true);

  // Lock Date
  const [lockDate, setLockDate] = useState<string>('');

  const [successMessage, setSuccessMessage] = useState<string>('');

  const { data: profile, isLoading } = useQuery<BusinessProfile>({
    queryKey: ['businessProfile'],
    queryFn: textileApi.getProfile,
  });

  const { data: gstStates } = useQuery<GstState[]>({
    queryKey: ['gstStates'],
    queryFn: textileApi.getGstStates,
  });

  useEffect(() => {
    if (profile) {
      setTradeName(profile.tradeName || '');
      setLegalName(profile.legalName || '');
      setAddress(profile.address || '');
      setStateCode(profile.stateCode || '33');
      setGstin(profile.gstin || '');
      setPan(profile.pan || '');
      setPhone(profile.phone || '');
      setEmail(profile.email || '');
      setBankDetailsText(profile.bankDetailsText || '');
      setInvoiceFooter(profile.invoiceFooter || '');
      setRoundTotal(profile.roundInvoiceTotal !== false);
      setLockDate(profile.lockDate || '');
    }
  }, [profile]);

  const updateProfileMutation = useMutation({
    mutationFn: () =>
      textileApi.updateProfile({
        tradeName,
        legalName,
        address,
        stateCode,
        gstin,
        pan,
        phone,
        email,
        bankDetailsText,
        invoiceFooter,
        roundInvoiceTotal: roundTotal,
        version: profile?.version,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['businessProfile'] });
      setSuccessMessage('Business Profile updated successfully!');
      setTimeout(() => setSuccessMessage(''), 4000);
    },
  });

  const updateLockDateMutation = useMutation({
    mutationFn: () => textileApi.updateLockDate(lockDate || null, profile?.version || 0),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['businessProfile'] });
      setSuccessMessage('Accounting Books Lock Date updated successfully!');
      setTimeout(() => setSuccessMessage(''), 4000);
    },
  });

  return (
    <Container size="xl" py="md">
      <Stack gap="lg">
        {/* Header */}
        <div>
          <Title order={2} fw={800} c="dark.8">
            System Settings &amp; Business Profile
          </Title>
          <Text size="sm" c="dimmed">
            Company legal credentials, GST filing registration, bank details, and accounting lock date protection
          </Text>
        </div>

        {successMessage && (
          <Alert color="teal" icon={<IconCheck size={18} />} title="Saved">
            {successMessage}
          </Alert>
        )}

        {/* Business Profile Card */}
        <Card shadow="xs" padding="lg" radius="md" withBorder bg="white">
          <Group justify="space-between" mb="md">
            <Group gap="xs">
              <IconBuildingStore size={22} color="#4338ca" />
              <Title order={4}>Business Profile &amp; GST Details</Title>
            </Group>
            <Badge color="indigo" variant="light">
              State: {stateCode}
            </Badge>
          </Group>
          <Divider mb="md" />

          {isLoading ? (
            <Text>Loading business profile...</Text>
          ) : (
            <Stack gap="md">
              <SimpleGrid cols={{ base: 1, sm: 2 }}>
                <TextInput
                  label="Trade / Brand Name"
                  placeholder="e.g. TEX_ARUN TEXTILES"
                  required
                  value={tradeName}
                  onChange={(e) => setTradeName(e.currentTarget.value)}
                />
                <TextInput
                  label="Legal Entity Name"
                  placeholder="e.g. TEX_ARUN WEAVING MILLS PVT LTD"
                  required
                  value={legalName}
                  onChange={(e) => setLegalName(e.currentTarget.value)}
                />
              </SimpleGrid>

              <SimpleGrid cols={{ base: 1, sm: 3 }}>
                <TextInput
                  label="GSTIN (15 characters)"
                  placeholder="e.g. 33AAAAA0000A1Z5"
                  maxLength={15}
                  value={gstin}
                  onChange={(e) => setGstin(e.currentTarget.value.toUpperCase())}
                />
                <Select
                  label="Registered GST State"
                  placeholder="Select State"
                  required
                  data={
                    gstStates?.map((s) => ({
                      value: s.code,
                      label: `${s.code} - ${s.name}`,
                    })) || []
                  }
                  value={stateCode}
                  onChange={(val) => setStateCode(val || '33')}
                  searchable
                />
                <TextInput
                  label="Permanent Account Number (PAN)"
                  placeholder="e.g. AAAAA0000A"
                  maxLength={10}
                  value={pan}
                  onChange={(e) => setPan(e.currentTarget.value.toUpperCase())}
                />
              </SimpleGrid>

              <SimpleGrid cols={{ base: 1, sm: 2 }}>
                <TextInput
                  label="Contact Phone"
                  placeholder="e.g. 9842100000"
                  required
                  value={phone}
                  onChange={(e) => setPhone(e.currentTarget.value)}
                />
                <TextInput
                  label="Contact Email"
                  placeholder="e.g. accounts@texarun.com"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.currentTarget.value)}
                />
              </SimpleGrid>

              <TextInput
                label="Registered Business Address"
                placeholder="Mill premises, street address, postal code"
                required
                value={address}
                onChange={(e) => setAddress(e.currentTarget.value)}
              />

              <SimpleGrid cols={{ base: 1, sm: 2 }}>
                <TextInput
                  label="Bank Payment Instructions (printed on invoices)"
                  placeholder="e.g. SBI Current A/c: 38920192839, IFSC: SBIN0001234"
                  value={bankDetailsText}
                  onChange={(e) => setBankDetailsText(e.currentTarget.value)}
                />
                <TextInput
                  label="Invoice Footer Note"
                  placeholder="e.g. Subject to Tiruppur Jurisdiction. Thank you for your business!"
                  value={invoiceFooter}
                  onChange={(e) => setInvoiceFooter(e.currentTarget.value)}
                />
              </SimpleGrid>

              <Switch
                label="Round off Invoice Totals to nearest whole Rupee"
                checked={roundTotal}
                onChange={(e) => setRoundTotal(e.currentTarget.checked)}
              />

              <Group justify="flex-end" mt="md">
                <Button
                  color="indigo"
                  onClick={() => updateProfileMutation.mutate()}
                  loading={updateProfileMutation.isPending}
                >
                  Save Business Profile
                </Button>
              </Group>
            </Stack>
          )}
        </Card>

        {/* Lock Date Card */}
        <Card shadow="xs" padding="lg" radius="md" withBorder bg="white">
          <Group justify="space-between" mb="md">
            <Group gap="xs">
              <IconLock size={22} color="#dc2626" />
              <Title order={4}>Accounting Books Lock Date</Title>
            </Group>
            <Badge color={profile?.lockDate ? 'red' : 'gray'} variant="light">
              {profile?.lockDate ? `Locked through ${formatDate(profile.lockDate)}` : 'Unlocked'}
            </Badge>
          </Group>
          <Divider mb="md" />

          <Stack gap="sm">
            <Text size="sm">
              Any sales invoices, purchase bills, payments, or journal entries on or before the lock date are
              strictly blocked by the server to prevent accidental changes to closed accounting periods.
            </Text>

            <Group align="flex-end">
              <TextInput
                label="Lock Date (Inclusive)"
                type="date"
                value={lockDate}
                onChange={(e) => setLockDate(e.currentTarget.value)}
                style={{ width: 220 }}
              />
              <Button
                color="red"
                onClick={() => updateLockDateMutation.mutate()}
                loading={updateLockDateMutation.isPending}
              >
                Set Lock Date
              </Button>
              {profile?.lockDate && (
                <Button
                  variant="subtle"
                  color="gray"
                  onClick={() => {
                    setLockDate('');
                    textileApi.updateLockDate(null, profile.version).then(() => {
                      queryClient.invalidateQueries({ queryKey: ['businessProfile'] });
                      setSuccessMessage('Lock date removed.');
                      setTimeout(() => setSuccessMessage(''), 4000);
                    });
                  }}
                >
                  Clear Lock
                </Button>
              )}
            </Group>
          </Stack>
        </Card>

        {/* Security Overview */}
        <Card shadow="xs" padding="lg" radius="md" withBorder bg="white">
          <Group justify="space-between" mb="sm">
            <Group gap="xs">
              <IconShieldCheck size={22} color="#16a34a" />
              <Title order={4}>Security &amp; Hard Engineering Protocol</Title>
            </Group>
            <Badge color="teal">Enforced</Badge>
          </Group>
          <Divider mb="md" />

          <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="md">
            <Card padding="sm" bg="gray.0" radius="sm">
              <Text fw={700} size="sm">
                Single Owner Session
              </Text>
              <Text size="xs" c="dimmed">
                Spring Session JDBC • 15m Brute-force Lockout • CSRF Cookie Validation
              </Text>
            </Card>

            <Card padding="sm" bg="gray.0" radius="sm">
              <Text fw={700} size="sm">
                Cryptographic Hashing
              </Text>
              <Text size="xs" c="dimmed">
                DelegatingPasswordEncoder with BCrypt (cost factor 12) default
              </Text>
            </Card>

            <Card padding="sm" bg="gray.0" radius="sm">
              <Text fw={700} size="sm">
                Ledger Immutability
              </Text>
              <Text size="xs" c="dimmed">
                PostgreSQL BEFORE UPDATE triggers enforce append-only immutable journal entries
              </Text>
            </Card>
          </SimpleGrid>
        </Card>
      </Stack>
    </Container>
  );
};
