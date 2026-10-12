import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage } from './pages/auth/LoginPage';
import { ProtectedRoute } from './components/auth/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { SalesInvoicesPage } from './pages/sales/SalesInvoicesPage';
import { PurchaseInvoicesPage } from './pages/purchases/PurchaseInvoicesPage';
import { PaymentsPage } from './pages/payments/PaymentsPage';
import { ExpensesPage } from './pages/expenses/ExpensesPage';
import { BankingPage } from './pages/banking/BankingPage';
import { PartiesPage } from './pages/parties/PartiesPage';
import { ItemMastersPage } from './pages/masters/ItemMastersPage';
import { ChartOfAccountsPage } from './pages/accounting/ChartOfAccountsPage';
import { ReportsPage } from './pages/reports/ReportsPage';
import { SettingsPage } from './pages/settings/SettingsPage';
import { AuditLogPage } from './pages/audit/AuditLogPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        {/* Protected routes wrapped in auth check and main responsive layout */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/sales" element={<SalesInvoicesPage />} />
            <Route path="/purchases" element={<PurchaseInvoicesPage />} />
            <Route path="/payments" element={<PaymentsPage />} />
            <Route path="/expenses" element={<ExpensesPage />} />
            <Route path="/banking" element={<BankingPage />} />
            <Route path="/parties" element={<PartiesPage />} />
            <Route path="/masters" element={<ItemMastersPage />} />
            <Route path="/accounting" element={<ChartOfAccountsPage />} />
            <Route path="/reports" element={<ReportsPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/audit-log" element={<AuditLogPage />} />
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
