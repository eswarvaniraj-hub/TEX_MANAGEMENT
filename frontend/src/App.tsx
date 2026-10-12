import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage } from './pages/auth/LoginPage';
import { ProtectedRoute } from './components/auth/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';
import { DashboardPage } from './pages/dashboard/DashboardPage';
import { ComingSoonPage } from './pages/common/ComingSoonPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        {/* Protected routes wrapped in auth check and main responsive layout */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route
              path="/sales"
              element={
                <ComingSoonPage
                  moduleName="Sales Invoicing"
                  phase="Phase 4"
                  description="GST tax invoices, delivery challans, rate management, and automatic double-entry journal postings."
                />
              }
            />
            <Route
              path="/purchases"
              element={
                <ComingSoonPage
                  moduleName="Purchase Bills"
                  phase="Phase 5"
                  description="Yarn and fabric purchase invoices, inward challans, supplier tax tracking, and stock intake."
                />
              }
            />
            <Route
              path="/payments"
              element={
                <ComingSoonPage
                  moduleName="Payments & Receipts"
                  phase="Phase 6"
                  description="Internal voucher recordings, customer payment collections, and supplier payouts with FIFO bill allocations."
                />
              }
            />
            <Route
              path="/expenses"
              element={
                <ComingSoonPage
                  moduleName="Expenses Management"
                  phase="Phase 6"
                  description="Direct and indirect operational expenses, petty cash disbursements, and tax categorization."
                />
              }
            />
            <Route
              path="/banking"
              element={
                <ComingSoonPage
                  moduleName="Banking & Cash"
                  phase="Phase 6"
                  description="Internal cashbook and bank account register with cheque clearance and statement matching."
                />
              }
            />
            <Route
              path="/parties"
              element={
                <ComingSoonPage
                  moduleName="Parties & GST Profiles"
                  phase="Phase 3"
                  description="Customer, supplier, weaver, and agent accounts with GSTIN validation and state code detection."
                />
              }
            />
            <Route
              path="/masters"
              element={
                <ComingSoonPage
                  moduleName="Item Masters & Catalog"
                  phase="Phase 3"
                  description="Yarn counts, grey cloth qualities, finished fabrics, and standard HSN classification codes."
                />
              }
            />
            <Route
              path="/accounting"
              element={
                <ComingSoonPage
                  moduleName="General Ledger & Daybook"
                  phase="Phase 7"
                  description="Immutable double-entry journal, daybook cash register, and account statement inquiries."
                />
              }
            />
            <Route
              path="/reports"
              element={
                <ComingSoonPage
                  moduleName="Reports & GST Filing"
                  phase="Phase 8"
                  description="Trial balance, Profit & Loss, Balance Sheet, and GSTR-1 / GSTR-3B monthly filing summaries."
                />
              }
            />
            <Route
              path="/settings"
              element={
                <ComingSoonPage
                  moduleName="System Settings"
                  phase="Phase 9"
                  description="Business profile configuration, financial year locking, and master system preferences."
                />
              }
            />
            <Route
              path="/audit-log"
              element={
                <ComingSoonPage
                  moduleName="Security Audit Log"
                  phase="Phase 9"
                  description="Complete immutable security audit trail of authentication events and data revisions."
                />
              }
            />
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
