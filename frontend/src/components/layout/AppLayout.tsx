import React from 'react';
import { Outlet, NavLink as RouterNavLink, useNavigate, useLocation } from 'react-router-dom';
import {
  AppShell,
  Burger,
  Group,
  Text,
  UnstyledButton,
  Badge,
  Button,
  ScrollArea,
  rem,
  Box,
  ThemeIcon,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import {
  IconDashboard,
  IconReceipt,
  IconShoppingCart,
  IconCash,
  IconReportMoney,
  IconBuildingBank,
  IconUsers,
  IconTags,
  IconBook2,
  IconChartBar,
  IconSettings,
  IconShieldLock,
  IconLogout,
  IconBuildingStore,
  IconUser,
  Icon,
} from '@tabler/icons-react';
import { useAuth } from '../../context/AuthContext';

interface NavItem {
  label: string;
  icon: Icon;
  path: string;
}

const navItems: NavItem[] = [
  { label: 'Dashboard', icon: IconDashboard, path: '/' },
  { label: 'Sales Invoices', icon: IconReceipt, path: '/sales' },
  { label: 'Purchases', icon: IconShoppingCart, path: '/purchases' },
  { label: 'Payments & Receipts', icon: IconCash, path: '/payments' },
  { label: 'Expenses', icon: IconReportMoney, path: '/expenses' },
  { label: 'Banking & Cash', icon: IconBuildingBank, path: '/banking' },
  { label: 'Parties & GST', icon: IconUsers, path: '/parties' },
  { label: 'Item Masters', icon: IconTags, path: '/masters' },
  { label: 'General Ledger', icon: IconBook2, path: '/accounting' },
  { label: 'Reports & GST Filing', icon: IconChartBar, path: '/reports' },
  { label: 'System Settings', icon: IconSettings, path: '/settings' },
  { label: 'Security Audit Log', icon: IconShieldLock, path: '/audit-log' },
];

export const AppLayout: React.FC = () => {
  const [opened, { toggle, close }] = useDisclosure();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{
        width: 260,
        breakpoint: 'sm',
        collapsed: { mobile: !opened },
      }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Group gap="sm">
            <Burger
              opened={opened}
              onClick={toggle}
              hiddenFrom="sm"
              size="sm"
              aria-label="Toggle navigation"
            />
            <ThemeIcon size={36} radius="md" color="indigo" variant="filled">
              <IconBuildingStore size={22} />
            </ThemeIcon>
            <div>
              <Text fw={800} size="md" c="indigo.8" style={{ lineHeight: 1.2 }}>
                TEX_ARUN
              </Text>
              <Text size="xs" c="dimmed" hiddenFrom="xs">
                Textile Accounts
              </Text>
              <Text size="xs" c="dimmed" visibleFrom="xs">
                Textile Accounts Management
              </Text>
            </div>
          </Group>

          <Group gap="xs">
            <Badge color="teal" variant="light" size="sm" visibleFrom="sm">
              FY 2026-27
            </Badge>

            <Group gap={6} px="xs" py={4} style={{ borderRadius: 6, backgroundColor: '#f1f5f9' }}>
              <IconUser size={14} color="#64748b" />
              <Text size="xs" fw={600} c="dark.7">
                {user?.username || 'Owner'}
              </Text>
              <Badge size="xs" color="indigo" variant="filled">
                {user?.role || 'OWNER'}
              </Badge>
            </Group>

            <Button
              variant="subtle"
              color="red"
              size="xs"
              leftSection={<IconLogout size={14} />}
              onClick={handleLogout}
            >
              Logout
            </Button>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="xs">
        <AppShell.Section grow component={ScrollArea}>
          <Box py="xs">
            {navItems.map((item) => {
              const isActive = location.pathname === item.path;
              const Icon = item.icon;
              return (
                <UnstyledButton
                  key={item.path}
                  component={RouterNavLink}
                  to={item.path}
                  onClick={close}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    width: '100%',
                    padding: `${rem(8)} ${rem(12)}`,
                    borderRadius: rem(6),
                    color: isActive ? '#4338ca' : '#475569',
                    backgroundColor: isActive ? '#e0e7ff' : 'transparent',
                    fontWeight: isActive ? 600 : 500,
                    fontSize: rem(13),
                    marginBottom: rem(4),
                    textDecoration: 'none',
                    transition: 'all 0.15s ease',
                  }}
                >
                  <Icon size={18} stroke={1.6} />
                  <span style={{ marginLeft: rem(12) }}>{item.label}</span>
                </UnstyledButton>
              );
            })}
          </Box>
        </AppShell.Section>
      </AppShell.Navbar>

      <AppShell.Main bg="#f8fafc">
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
};
