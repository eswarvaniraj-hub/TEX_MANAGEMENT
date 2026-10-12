import { apiClient } from './client';

export interface GstState {
  code: string;
  name: string;
}

export interface Unit {
  id: number;
  code: string;
  name: string;
  decimalPlaces: number;
  active: boolean;
  version: number;
}

export interface TaxRate {
  id: number;
  name: string;
  ratePercent: number;
  active: boolean;
  version: number;
}

export interface Product {
  id: number;
  code: string;
  name: string;
  productType: string;
  unitId: number;
  unitCode: string;
  unitDecimals: number;
  hsnCode?: string;
  defaultTaxRateId?: number;
  defaultTaxRatePercent?: number;
  defaultSaleRate?: number;
  defaultPurchaseRate?: number;
  description?: string;
  active: boolean;
  version: number;
}

export interface Customer {
  id: number;
  code: string;
  name: string;
  contactPerson?: string;
  phone?: string;
  email?: string;
  address?: string;
  city?: string;
  stateCode: string;
  stateName: string;
  gstin?: string;
  pan?: string;
  notes?: string;
  active: boolean;
  version: number;
}

export interface Supplier {
  id: number;
  code: string;
  name: string;
  supplierType?: string;
  contactPerson?: string;
  phone?: string;
  email?: string;
  address?: string;
  city?: string;
  stateCode: string;
  stateName: string;
  gstin?: string;
  pan?: string;
  notes?: string;
  active: boolean;
  version: number;
}

export interface PaymentAccount {
  id: number;
  name: string;
  kind: 'CASH' | 'BANK';
  accountId: number;
  accountCode: string;
  accountName: string;
  bankName?: string;
  accountLast4?: string;
  active: boolean;
  currentBalance: number | string;
  version: number;
}

export interface ExpenseCategory {
  id: number;
  name: string;
  accountId: number;
  accountCode: string;
  accountName: string;
  active: boolean;
  version: number;
}

export interface BusinessProfile {
  id: number;
  legalName: string;
  tradeName: string;
  address: string;
  stateCode: string;
  gstin?: string;
  pan?: string;
  phone: string;
  email: string;
  invoiceFooter?: string;
  bankDetailsText?: string;
  gstRegistered: boolean;
  roundInvoiceTotal: boolean;
  lockDate?: string;
  version: number;
}

export interface AccountItem {
  id: number;
  code: string;
  name: string;
  accountType: string;
  systemRole?: string;
  reportGroup?: string;
  parentId?: number;
  parentName?: string;
  isPostable: boolean;
  isSystem: boolean;
  active: boolean;
  balance: number | string;
  version: number;
}

export interface InvoiceLine {
  lineNo?: number;
  productId: number;
  productName?: string;
  description?: string;
  hsnCode?: string;
  quantity: number | string;
  unitId: number;
  unitCode?: string;
  rate: number | string;
  lineAmount?: number | string;
  discountAmount?: number | string;
  taxableValue?: number | string;
  taxRatePercent?: number | string;
  cgst?: number | string;
  sgst?: number | string;
  igst?: number | string;
  lineTotal?: number | string;
}

export interface InvoiceCalculationResponse {
  isInterstate: boolean;
  subtotal: number | string;
  discountTotal: number | string;
  taxableTotal: number | string;
  cgstTotal: number | string;
  sgstTotal: number | string;
  igstTotal: number | string;
  roundOff: number | string;
  grandTotal: number | string;
  lines: InvoiceLine[];
}

export interface SalesInvoice {
  id: number;
  invoiceNo?: string;
  customerId: number;
  customerName: string;
  customerGstin?: string;
  customerAddress?: string;
  invoiceDate: string;
  dueDate?: string;
  placeOfSupplyStateCode: string;
  placeOfSupplyStateName: string;
  isInterstate: boolean;
  status: 'DRAFT' | 'POSTED' | 'REVERSED';
  subtotal: number | string;
  discountTotal: number | string;
  taxableTotal: number | string;
  cgstTotal: number | string;
  sgstTotal: number | string;
  igstTotal: number | string;
  roundOff: number | string;
  grandTotal: number | string;
  notes?: string;
  journalEntryId?: number;
  reversalEntryId?: number;
  postedAt?: string;
  postedBy?: string;
  reversedAt?: string;
  reversalReason?: string;
  version: number;
  lines: InvoiceLine[];
}

export interface PurchaseInvoice {
  id: number;
  voucherNo?: string;
  supplierId: number;
  supplierName: string;
  supplierGstin?: string;
  supplierAddress?: string;
  supplierInvoiceNo: string;
  supplierInvoiceDate: string;
  purchaseDate: string;
  status: 'DRAFT' | 'POSTED' | 'REVERSED';
  subtotal: number | string;
  discountTotal: number | string;
  taxableTotal: number | string;
  cgstTotal: number | string;
  sgstTotal: number | string;
  igstTotal: number | string;
  roundOff: number | string;
  grandTotal: number | string;
  notes?: string;
  journalEntryId?: number;
  reversalEntryId?: number;
  postedAt?: string;
  postedBy?: string;
  reversedAt?: string;
  reversalReason?: string;
  version: number;
  lines: InvoiceLine[];
}

export interface PaymentItem {
  id: number;
  paymentNo: string;
  direction: 'RECEIPT' | 'PAYMENT';
  customerId?: number;
  customerName?: string;
  supplierId?: number;
  supplierName?: string;
  paymentDate: string;
  paymentAccountId: number;
  paymentAccountName: string;
  amount: number | string;
  method: string;
  referenceNo?: string;
  notes?: string;
  status: 'POSTED' | 'REVERSED';
  journalEntryId?: number;
  reversalEntryId?: number;
  reversedAt?: string;
  reversalReason?: string;
}

export interface ExpenseItem {
  id: number;
  expenseNo: string;
  expenseDate: string;
  categoryId: number;
  categoryName: string;
  description: string;
  amount: number | string;
  paymentAccountId: number;
  paymentAccountName: string;
  referenceNo?: string;
  notes?: string;
  status: 'POSTED' | 'REVERSED';
}

export interface FundTransferItem {
  id: number;
  transferNo: string;
  transferDate: string;
  fromPaymentAccountId: number;
  fromPaymentAccountName: string;
  toPaymentAccountId: number;
  toPaymentAccountName: string;
  amount: number | string;
  reference?: string;
  notes?: string;
  status: 'POSTED' | 'REVERSED';
}

export interface DashboardData {
  kpis: {
    todaySales: number | string;
    thisMonthSales: number | string;
    customerOutstanding: number | string;
    supplierOutstanding: number | string;
    cashBalance: number | string;
    bankBalance: number | string;
  };
  recentSales: SalesInvoice[];
  recentPurchases: PurchaseInvoice[];
}

export interface TrialBalanceReport {
  asOfDate: string;
  totalDebits: number | string;
  totalCredits: number | string;
  difference: number | string;
  balanced: boolean;
  lines: {
    accountId: number;
    code: string;
    name: string;
    accountType: string;
    debit: number | string;
    credit: number | string;
  }[];
}

export interface DayBookLine {
  entryNo: string;
  date: string;
  accountCode: string;
  accountName: string;
  partyName: string;
  narration: string;
  debit: number | string;
  credit: number | string;
}

export interface PartyLedgerLine {
  date: string;
  entryNo: string;
  description: string;
  debit: number | string;
  credit: number | string;
  runningBalance: number | string;
}

export interface IntegrityCheckResult {
  healthy: boolean;
  totalEntriesChecked: number;
  totalDebits: number | string;
  totalCredits: number | string;
  netDifference: number | string;
  violations: string[];
}

export interface AuditEventItem {
  id: number;
  occurredAt: string;
  actorUserId?: number;
  actorUsername: string;
  action: string;
  entityType: string;
  entityId?: string;
  summary: string;
  ipAddress?: string;
}

export interface PaginatedResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ======================== API Methods ========================

export const textileApi = {
  // Master data
  getGstStates: () => apiClient<GstState[]>('/api/v1/gst-states'),
  getUnits: (active?: boolean) => apiClient<Unit[]>(`/api/v1/units${active !== undefined ? `?active=${active}` : ''}`),
  createUnit: (data: Partial<Unit>) => apiClient<Unit>('/api/v1/units', { method: 'POST', body: JSON.stringify(data) }),
  getTaxRates: (active?: boolean) => apiClient<TaxRate[]>(`/api/v1/tax-rates${active !== undefined ? `?active=${active}` : ''}`),
  createTaxRate: (data: Partial<TaxRate>) => apiClient<TaxRate>('/api/v1/tax-rates', { method: 'POST', body: JSON.stringify(data) }),
  getProducts: (params?: { q?: string; active?: boolean; page?: number; size?: number }) => {
    const qp = new URLSearchParams();
    if (params?.q) qp.set('q', params.q);
    if (params?.active !== undefined) qp.set('active', String(params.active));
    if (params?.page !== undefined) qp.set('page', String(params.page));
    if (params?.size !== undefined) qp.set('size', String(params.size));
    return apiClient<PaginatedResult<Product>>(`/api/v1/products?${qp.toString()}`);
  },
  createProduct: (data: Partial<Product>) => apiClient<Product>('/api/v1/products', { method: 'POST', body: JSON.stringify(data) }),
  updateProduct: (id: number, data: Partial<Product>, version?: number) =>
    apiClient<Product>(`/api/v1/products/${id}${version !== undefined ? `?version=${version}` : ''}`, { method: 'PUT', body: JSON.stringify(data) }),

  getCustomers: (params?: { q?: string; active?: boolean; page?: number; size?: number }) => {
    const qp = new URLSearchParams();
    if (params?.q) qp.set('q', params.q);
    if (params?.active !== undefined) qp.set('active', String(params.active));
    if (params?.page !== undefined) qp.set('page', String(params.page));
    if (params?.size !== undefined) qp.set('size', String(params.size));
    return apiClient<PaginatedResult<Customer>>(`/api/v1/customers?${qp.toString()}`);
  },
  createCustomer: (data: Partial<Customer>) => apiClient<Customer>('/api/v1/customers', { method: 'POST', body: JSON.stringify(data) }),
  updateCustomer: (id: number, data: Partial<Customer>, version?: number) =>
    apiClient<Customer>(`/api/v1/customers/${id}${version !== undefined ? `?version=${version}` : ''}`, { method: 'PUT', body: JSON.stringify(data) }),

  getSuppliers: (params?: { q?: string; active?: boolean; page?: number; size?: number }) => {
    const qp = new URLSearchParams();
    if (params?.q) qp.set('q', params.q);
    if (params?.active !== undefined) qp.set('active', String(params.active));
    if (params?.page !== undefined) qp.set('page', String(params.page));
    if (params?.size !== undefined) qp.set('size', String(params.size));
    return apiClient<PaginatedResult<Supplier>>(`/api/v1/suppliers?${qp.toString()}`);
  },
  createSupplier: (data: Partial<Supplier>) => apiClient<Supplier>('/api/v1/suppliers', { method: 'POST', body: JSON.stringify(data) }),
  updateSupplier: (id: number, data: Partial<Supplier>, version?: number) =>
    apiClient<Supplier>(`/api/v1/suppliers/${id}${version !== undefined ? `?version=${version}` : ''}`, { method: 'PUT', body: JSON.stringify(data) }),

  // Settings
  getProfile: () => apiClient<BusinessProfile>('/api/v1/settings/profile'),
  updateProfile: (data: Partial<BusinessProfile>) => apiClient<BusinessProfile>('/api/v1/settings/profile', { method: 'PUT', body: JSON.stringify(data) }),
  updateLockDate: (lockDate: string | null, version: number) =>
    apiClient<BusinessProfile>('/api/v1/settings/lock-date', { method: 'PUT', body: JSON.stringify({ lockDate, version }) }),

  // Accounts & Banking
  getAccounts: () => apiClient<AccountItem[]>('/api/v1/accounts/all'),
  createAccount: (data: Partial<AccountItem>) => apiClient<AccountItem>('/api/v1/accounts', { method: 'POST', body: JSON.stringify(data) }),
  getPaymentAccounts: () => apiClient<PaymentAccount[]>('/api/v1/payment-accounts/active'),
  createPaymentAccount: (data: Partial<PaymentAccount>) =>
    apiClient<PaymentAccount>('/api/v1/payment-accounts', { method: 'POST', body: JSON.stringify(data) }),
  getExpenseCategories: () => apiClient<ExpenseCategory[]>('/api/v1/expense-categories/active'),
  createExpenseCategory: (data: Partial<ExpenseCategory>) =>
    apiClient<ExpenseCategory>('/api/v1/expense-categories', { method: 'POST', body: JSON.stringify(data) }),
  getFundTransfers: (params?: { from?: string; to?: string; page?: number; size?: number }) => {
    const qp = new URLSearchParams();
    if (params?.from) qp.set('from', params.from);
    if (params?.to) qp.set('to', params.to);
    return apiClient<PaginatedResult<FundTransferItem>>(`/api/v1/banking/transfers?${qp.toString()}`);
  },
  createFundTransfer: (data: { transferDate: string; fromPaymentAccountId: number; toPaymentAccountId: number; amount: number | string; reference?: string; notes?: string }) =>
    apiClient<FundTransferItem>('/api/v1/banking/transfers', { method: 'POST', body: JSON.stringify(data) }),
  reverseFundTransfer: (id: number, reason: string) =>
    apiClient<FundTransferItem>(`/api/v1/banking/transfers/${id}/reverse`, { method: 'POST', body: JSON.stringify({ reason }) }),
  getAccountStatement: (paymentAccountId: number, from?: string, to?: string) => {
    const qp = new URLSearchParams();
    if (from) qp.set('from', from);
    if (to) qp.set('to', to);
    return apiClient<{ date: string; entryNo: string; description: string; debit: number; credit: number; runningBalance: number }[]>(
      `/api/v1/banking/accounts/${paymentAccountId}/statement?${qp.toString()}`
    );
  },

  // Sales
  getSalesInvoices: (params?: { customerId?: number; status?: string; from?: string; to?: string; q?: string; page?: number; size?: number }) => {
    const qp = new URLSearchParams();
    if (params?.customerId) qp.set('customerId', String(params.customerId));
    if (params?.status) qp.set('status', params.status);
    if (params?.from) qp.set('from', params.from);
    if (params?.to) qp.set('to', params.to);
    if (params?.q) qp.set('q', params.q);
    if (params?.page !== undefined) qp.set('page', String(params.page));
    if (params?.size !== undefined) qp.set('size', String(params.size));
    return apiClient<PaginatedResult<SalesInvoice>>(`/api/v1/sales-invoices?${qp.toString()}`);
  },
  getSalesInvoice: (id: number) => apiClient<SalesInvoice>(`/api/v1/sales-invoices/${id}`),
  calculateSalesInvoice: (data: unknown) => apiClient<InvoiceCalculationResponse>('/api/v1/sales-invoices/calculate', { method: 'POST', body: JSON.stringify(data) }),
  createSalesInvoice: (data: unknown) => apiClient<SalesInvoice>('/api/v1/sales-invoices', { method: 'POST', body: JSON.stringify(data) }),
  updateSalesInvoice: (id: number, data: unknown, version?: number) =>
    apiClient<SalesInvoice>(`/api/v1/sales-invoices/${id}${version !== undefined ? `?version=${version}` : ''}`, { method: 'PUT', body: JSON.stringify(data) }),
  postSalesInvoice: (id: number) => apiClient<SalesInvoice>(`/api/v1/sales-invoices/${id}/post`, { method: 'POST' }),
  reverseSalesInvoice: (id: number, reason: string) =>
    apiClient<SalesInvoice>(`/api/v1/sales-invoices/${id}/reverse`, { method: 'POST', body: JSON.stringify({ reason }) }),

  // Purchases
  getPurchaseInvoices: (params?: { supplierId?: number; status?: string; from?: string; to?: string; q?: string; page?: number; size?: number }) => {
    const qp = new URLSearchParams();
    if (params?.supplierId) qp.set('supplierId', String(params.supplierId));
    if (params?.status) qp.set('status', params.status);
    if (params?.from) qp.set('from', params.from);
    if (params?.to) qp.set('to', params.to);
    if (params?.q) qp.set('q', params.q);
    if (params?.page !== undefined) qp.set('page', String(params.page));
    if (params?.size !== undefined) qp.set('size', String(params.size));
    return apiClient<PaginatedResult<PurchaseInvoice>>(`/api/v1/purchase-invoices?${qp.toString()}`);
  },
  getPurchaseInvoice: (id: number) => apiClient<PurchaseInvoice>(`/api/v1/purchase-invoices/${id}`),
  calculatePurchaseInvoice: (data: unknown) => apiClient<InvoiceCalculationResponse>('/api/v1/purchase-invoices/calculate', { method: 'POST', body: JSON.stringify(data) }),
  createPurchaseInvoice: (data: unknown) => apiClient<PurchaseInvoice>('/api/v1/purchase-invoices', { method: 'POST', body: JSON.stringify(data) }),
  updatePurchaseInvoice: (id: number, data: unknown, version?: number) =>
    apiClient<PurchaseInvoice>(`/api/v1/purchase-invoices/${id}${version !== undefined ? `?version=${version}` : ''}`, { method: 'PUT', body: JSON.stringify(data) }),
  postPurchaseInvoice: (id: number) => apiClient<PurchaseInvoice>(`/api/v1/purchase-invoices/${id}/post`, { method: 'POST' }),
  reversePurchaseInvoice: (id: number, reason: string) =>
    apiClient<PurchaseInvoice>(`/api/v1/purchase-invoices/${id}/reverse`, { method: 'POST', body: JSON.stringify({ reason }) }),

  // Payments & Receipts
  getPayments: (params?: { direction?: string; partyId?: number; from?: string; to?: string; page?: number; size?: number }) => {
    const qp = new URLSearchParams();
    if (params?.direction) qp.set('direction', params.direction);
    if (params?.partyId) qp.set('partyId', String(params.partyId));
    if (params?.from) qp.set('from', params.from);
    if (params?.to) qp.set('to', params.to);
    if (params?.page !== undefined) qp.set('page', String(params.page));
    if (params?.size !== undefined) qp.set('size', String(params.size));
    return apiClient<PaginatedResult<PaymentItem>>(`/api/v1/payments?${qp.toString()}`);
  },
  createPayment: (data: unknown) => apiClient<PaymentItem>('/api/v1/payments', { method: 'POST', body: JSON.stringify(data) }),
  reversePayment: (id: number, reason: string) =>
    apiClient<PaymentItem>(`/api/v1/payments/${id}/reverse`, { method: 'POST', body: JSON.stringify({ reason }) }),

  // Expenses
  getExpenses: (params?: { from?: string; to?: string; categoryId?: number; status?: string; page?: number; size?: number }) => {
    const qp = new URLSearchParams();
    if (params?.from) qp.set('from', params.from);
    if (params?.to) qp.set('to', params.to);
    if (params?.categoryId) qp.set('categoryId', String(params.categoryId));
    if (params?.status) qp.set('status', params.status);
    if (params?.page !== undefined) qp.set('page', String(params.page));
    if (params?.size !== undefined) qp.set('size', String(params.size));
    return apiClient<PaginatedResult<ExpenseItem>>(`/api/v1/expenses?${qp.toString()}`);
  },
  createExpense: (data: unknown) => apiClient<ExpenseItem>('/api/v1/expenses', { method: 'POST', body: JSON.stringify(data) }),
  reverseExpense: (id: number, reason: string) =>
    apiClient<ExpenseItem>(`/api/v1/expenses/${id}/reverse`, { method: 'POST', body: JSON.stringify({ reason }) }),

  // Dashboard & Reports & Ledger
  getDashboard: () => apiClient<DashboardData>('/api/v1/dashboard'),
  getTrialBalance: (asOfDate?: string) => apiClient<TrialBalanceReport>(`/api/v1/reports/trial-balance${asOfDate ? `?asOfDate=${asOfDate}` : ''}`),
  getDayBook: (date?: string) => apiClient<DayBookLine[]>(`/api/v1/reports/day-book${date ? `?date=${date}` : ''}`),
  getCustomerLedger: (id: number, from?: string, to?: string) => {
    const qp = new URLSearchParams();
    if (from) qp.set('from', from);
    if (to) qp.set('to', to);
    return apiClient<PartyLedgerLine[]>(`/api/v1/ledger/customers/${id}/lines?${qp.toString()}`);
  },
  getSupplierLedger: (id: number, from?: string, to?: string) => {
    const qp = new URLSearchParams();
    if (from) qp.set('from', from);
    if (to) qp.set('to', to);
    return apiClient<PartyLedgerLine[]>(`/api/v1/ledger/suppliers/${id}/lines?${qp.toString()}`);
  },
  getIntegrityCheck: () => apiClient<IntegrityCheckResult>('/api/v1/ledger/integrity-check'),
  getAuditEvents: (limit?: number) =>
    apiClient<AuditEventItem[]>(`/api/v1/audit-events${limit ? `?limit=${limit}` : ''}`),
};
