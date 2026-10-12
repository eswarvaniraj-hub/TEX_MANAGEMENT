import React, { useState } from 'react';
import {
  Container,
  Title,
  Text,
  Group,
  Button,
  Card,
  Table,
  Badge,
  ActionIcon,
  Modal,
  TextInput,
  Select,
  NumberInput,
  Stack,
  Tabs,
  SimpleGrid,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  IconPlus,
  IconEdit,
  IconTags,
  IconRuler,
  IconPercentage,
  IconCategory,
} from '@tabler/icons-react';
import { textileApi, Product, Unit, TaxRate, ExpenseCategory } from '../../api/textileApi';
import { formatCurrency } from '../../utils/formatters';

export const ItemMastersPage: React.FC = () => {
  const queryClient = useQueryClient();

  const [activeTab, setActiveTab] = useState<string>('products');

  const [prodModalOpened, { open: openProdModal, close: closeProdModal }] = useDisclosure(false);
  const [unitModalOpened, { open: openUnitModal, close: closeUnitModal }] = useDisclosure(false);
  const [taxModalOpened, { open: openTaxModal, close: closeTaxModal }] = useDisclosure(false);
  const [catModalOpened, { open: openCatModal, close: closeCatModal }] = useDisclosure(false);

  const [editingProductId, setEditingProductId] = useState<number | null>(null);

  // Product Form
  const [prodName, setProdName] = useState<string>('');
  const [prodType, setProdType] = useState<string>('FABRIC');
  const [prodUnitId, setProdUnitId] = useState<string>('');
  const [prodHsn, setProdHsn] = useState<string>('5208');
  const [prodTaxId, setProdTaxId] = useState<string>('');
  const [prodSaleRate, setProdSaleRate] = useState<number>(0);
  const [prodPurchaseRate, setProdPurchaseRate] = useState<number>(0);

  // Unit Form
  const [unitCode, setUnitCode] = useState<string>('');
  const [unitName, setUnitName] = useState<string>('');
  const [unitDecimals, setUnitDecimals] = useState<number>(2);

  // Tax Rate Form
  const [taxName, setTaxName] = useState<string>('');
  const [taxPercent, setTaxPercent] = useState<number>(5);

  // Expense Category Form
  const [catName, setCatName] = useState<string>('');

  // Queries
  const { data: productsData, isLoading: prodsLoading } = useQuery({
    queryKey: ['productsMaster'],
    queryFn: () => textileApi.getProducts({ size: 100 }),
  });

  const { data: units, isLoading: unitsLoading } = useQuery({
    queryKey: ['unitsList'],
    queryFn: () => textileApi.getUnits(),
  });

  const { data: taxRates, isLoading: taxesLoading } = useQuery({
    queryKey: ['taxRatesList'],
    queryFn: () => textileApi.getTaxRates(),
  });

  const { data: expenseCats, isLoading: catsLoading } = useQuery({
    queryKey: ['expenseCategories'],
    queryFn: textileApi.getExpenseCategories,
  });

  // Mutations
  const saveProductMutation = useMutation({
    mutationFn: () => {
      const payload = {
        name: prodName,
        productType: prodType,
        unitId: Number(prodUnitId),
        hsnCode: prodHsn,
        defaultTaxRateId: prodTaxId ? Number(prodTaxId) : undefined,
        defaultSaleRate: prodSaleRate,
        defaultPurchaseRate: prodPurchaseRate,
      };
      return editingProductId
        ? textileApi.updateProduct(editingProductId, payload)
        : textileApi.createProduct(payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['productsMaster'] });
      closeProdModal();
    },
  });

  const createUnitMutation = useMutation({
    mutationFn: () =>
      textileApi.createUnit({
        code: unitCode,
        name: unitName,
        decimalPlaces: unitDecimals,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['unitsList'] });
      closeUnitModal();
      setUnitCode('');
      setUnitName('');
    },
  });

  const createTaxMutation = useMutation({
    mutationFn: () =>
      textileApi.createTaxRate({
        name: taxName,
        ratePercent: taxPercent,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['taxRatesList'] });
      closeTaxModal();
      setTaxName('');
      setTaxPercent(5);
    },
  });

  const createCatMutation = useMutation({
    mutationFn: () => textileApi.createExpenseCategory({ name: catName }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['expenseCategories'] });
      closeCatModal();
      setCatName('');
    },
  });

  const handleOpenProductCreate = () => {
    setEditingProductId(null);
    setProdName('');
    setProdType('FABRIC');
    setProdUnitId(units && units.length > 0 ? String(units[0].id) : '');
    setProdHsn('5208');
    setProdTaxId(taxRates && taxRates.length > 0 ? String(taxRates[0].id) : '');
    setProdSaleRate(150);
    setProdPurchaseRate(120);
    openProdModal();
  };

  const handleOpenProductEdit = (p: Product) => {
    setEditingProductId(p.id);
    setProdName(p.name);
    setProdType(p.productType);
    setProdUnitId(String(p.unitId));
    setProdHsn(p.hsnCode || '5208');
    setProdTaxId(p.defaultTaxRateId ? String(p.defaultTaxRateId) : '');
    setProdSaleRate(Number(p.defaultSaleRate) || 0);
    setProdPurchaseRate(Number(p.defaultPurchaseRate) || 0);
    openProdModal();
  };

  return (
    <Container size="xl" py="md">
      <Stack gap="lg">
        {/* Header */}
        <Group justify="space-between" align="center">
          <div>
            <Title order={2} fw={800} c="dark.8">
              Item Masters &amp; Catalog
            </Title>
            <Text size="sm" c="dimmed">
              Yarn counts, fabrics, units of measurement, GST tax slabs, and operational expense categories
            </Text>
          </div>
          {activeTab === 'products' && (
            <Button color="indigo" size="sm" leftSection={<IconPlus size={16} />} onClick={handleOpenProductCreate}>
              + Add Product / Quality
            </Button>
          )}
          {activeTab === 'units' && (
            <Button color="indigo" size="sm" leftSection={<IconPlus size={16} />} onClick={openUnitModal}>
              + Add Unit
            </Button>
          )}
          {activeTab === 'taxes' && (
            <Button color="indigo" size="sm" leftSection={<IconPlus size={16} />} onClick={openTaxModal}>
              + Add Tax Rate
            </Button>
          )}
          {activeTab === 'expenses' && (
            <Button color="indigo" size="sm" leftSection={<IconPlus size={16} />} onClick={openCatModal}>
              + Add Expense Category
            </Button>
          )}
        </Group>

        {/* Master Tabs */}
        <Tabs value={activeTab} onChange={(val) => setActiveTab(val || 'products')}>
          <Tabs.List>
            <Tabs.Tab value="products" leftSection={<IconTags size={16} />}>
              Products &amp; Fabrics
            </Tabs.Tab>
            <Tabs.Tab value="units" leftSection={<IconRuler size={16} />}>
              Units of Measure
            </Tabs.Tab>
            <Tabs.Tab value="taxes" leftSection={<IconPercentage size={16} />}>
              GST Tax Rates
            </Tabs.Tab>
            <Tabs.Tab value="expenses" leftSection={<IconCategory size={16} />}>
              Expense Categories
            </Tabs.Tab>
          </Tabs.List>
        </Tabs>

        {/* Tab Content: Products */}
        {activeTab === 'products' && (
          <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
            <Table.ScrollContainer minWidth={750}>
              <Table striped highlightOnHover verticalSpacing="sm" fz="sm">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Code</Table.Th>
                    <Table.Th>Product Name</Table.Th>
                    <Table.Th>Type</Table.Th>
                    <Table.Th>HSN Code</Table.Th>
                    <Table.Th>Unit</Table.Th>
                    <Table.Th ta="right">Default Sale (₹)</Table.Th>
                    <Table.Th ta="right">Default Purchase (₹)</Table.Th>
                    <Table.Th>Tax %</Table.Th>
                    <Table.Th ta="center">Actions</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {prodsLoading ? (
                    <Table.Tr>
                      <Table.Td colSpan={9} ta="center" py="xl">
                        Loading products...
                      </Table.Td>
                    </Table.Tr>
                  ) : !productsData?.content || productsData.content.length === 0 ? (
                    <Table.Tr>
                      <Table.Td colSpan={9} ta="center" py="xl" c="dimmed">
                        No products configured. Click &quot;+ Add Product&quot; to begin.
                      </Table.Td>
                    </Table.Tr>
                  ) : (
                    productsData.content.map((p: Product) => (
                      <Table.Tr key={p.id}>
                        <Table.Td fw={700} c="indigo.8">
                          {p.code}
                        </Table.Td>
                        <Table.Td fw={600}>{p.name}</Table.Td>
                        <Table.Td>
                          <Badge size="xs" variant="light" color="indigo">
                            {p.productType}
                          </Badge>
                        </Table.Td>
                        <Table.Td>{p.hsnCode || '-'}</Table.Td>
                        <Table.Td>{p.unitCode}</Table.Td>
                        <Table.Td ta="right">{formatCurrency(p.defaultSaleRate)}</Table.Td>
                        <Table.Td ta="right">{formatCurrency(p.defaultPurchaseRate)}</Table.Td>
                        <Table.Td>{p.defaultTaxRatePercent || 0}%</Table.Td>
                        <Table.Td ta="center">
                          <ActionIcon
                            size="sm"
                            variant="light"
                            color="blue"
                            onClick={() => handleOpenProductEdit(p)}
                          >
                            <IconEdit size={15} />
                          </ActionIcon>
                        </Table.Td>
                      </Table.Tr>
                    ))
                  )}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          </Card>
        )}

        {/* Tab Content: Units */}
        {activeTab === 'units' && (
          <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
            <Table.ScrollContainer minWidth={500}>
              <Table striped highlightOnHover verticalSpacing="sm" fz="sm">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Code</Table.Th>
                    <Table.Th>Unit Name</Table.Th>
                    <Table.Th>Decimal Places</Table.Th>
                    <Table.Th>Status</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {unitsLoading ? (
                    <Table.Tr>
                      <Table.Td colSpan={4} ta="center" py="xl">
                        Loading units...
                      </Table.Td>
                    </Table.Tr>
                  ) : (
                    units?.map((u: Unit) => (
                      <Table.Tr key={u.id}>
                        <Table.Td fw={700} c="indigo.8">
                          {u.code}
                        </Table.Td>
                        <Table.Td fw={600}>{u.name}</Table.Td>
                        <Table.Td>{u.decimalPlaces} decimals</Table.Td>
                        <Table.Td>
                          <Badge size="xs" color={u.active ? 'teal' : 'gray'}>
                            {u.active ? 'Active' : 'Inactive'}
                          </Badge>
                        </Table.Td>
                      </Table.Tr>
                    ))
                  )}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          </Card>
        )}

        {/* Tab Content: Tax Rates */}
        {activeTab === 'taxes' && (
          <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
            <Table.ScrollContainer minWidth={500}>
              <Table striped highlightOnHover verticalSpacing="sm" fz="sm">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Slab Name</Table.Th>
                    <Table.Th>Rate Percent</Table.Th>
                    <Table.Th>CGST + SGST Split</Table.Th>
                    <Table.Th>Status</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {taxesLoading ? (
                    <Table.Tr>
                      <Table.Td colSpan={4} ta="center" py="xl">
                        Loading tax rates...
                      </Table.Td>
                    </Table.Tr>
                  ) : (
                    taxRates?.map((t: TaxRate) => (
                      <Table.Tr key={t.id}>
                        <Table.Td fw={700}>{t.name}</Table.Td>
                        <Table.Td fw={800} c="indigo.8">
                          {t.ratePercent}%
                        </Table.Td>
                        <Table.Td>
                          {Number(t.ratePercent) / 2}% CGST + {Number(t.ratePercent) / 2}% SGST
                        </Table.Td>
                        <Table.Td>
                          <Badge size="xs" color={t.active ? 'teal' : 'gray'}>
                            {t.active ? 'Active' : 'Inactive'}
                          </Badge>
                        </Table.Td>
                      </Table.Tr>
                    ))
                  )}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          </Card>
        )}

        {/* Tab Content: Expense Categories */}
        {activeTab === 'expenses' && (
          <Card shadow="xs" padding="md" radius="md" withBorder bg="white">
            <Table.ScrollContainer minWidth={500}>
              <Table striped highlightOnHover verticalSpacing="sm" fz="sm">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Category Name</Table.Th>
                    <Table.Th>Linked Ledger Account</Table.Th>
                    <Table.Th>Status</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {catsLoading ? (
                    <Table.Tr>
                      <Table.Td colSpan={3} ta="center" py="xl">
                        Loading expense categories...
                      </Table.Td>
                    </Table.Tr>
                  ) : (
                    expenseCats?.map((c: ExpenseCategory) => (
                      <Table.Tr key={c.id}>
                        <Table.Td fw={700}>{c.name}</Table.Td>
                        <Table.Td>
                          {c.accountName} ({c.accountCode})
                        </Table.Td>
                        <Table.Td>
                          <Badge size="xs" color={c.active ? 'teal' : 'gray'}>
                            {c.active ? 'Active' : 'Inactive'}
                          </Badge>
                        </Table.Td>
                      </Table.Tr>
                    ))
                  )}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
          </Card>
        )}
      </Stack>

      {/* Product Modal */}
      <Modal
        opened={prodModalOpened}
        onClose={closeProdModal}
        title={editingProductId ? 'Edit Product' : 'Add New Product / Fabric'}
        size="md"
      >
        <Stack gap="sm">
          <TextInput
            label="Product Name"
            placeholder="e.g. 40s Cotton Grey Fabric"
            required
            value={prodName}
            onChange={(e) => setProdName(e.currentTarget.value)}
          />

          <SimpleGrid cols={2}>
            <Select
              label="Type"
              required
              data={[
                { value: 'FABRIC', label: 'Fabric / Cloth' },
                { value: 'YARN', label: 'Yarn' },
                { value: 'RAW_MATERIAL', label: 'Raw Material' },
                { value: 'SERVICE', label: 'Jobwork / Service' },
                { value: 'OTHER', label: 'Other' },
              ]}
              value={prodType}
              onChange={(val) => setProdType(val || 'FABRIC')}
            />
            <Select
              label="Unit of Measure"
              required
              data={
                units?.map((u: Unit) => ({
                  value: String(u.id),
                  label: `${u.name} (${u.code})`,
                })) || []
              }
              value={prodUnitId}
              onChange={(val) => setProdUnitId(val || '')}
            />
          </SimpleGrid>

          <SimpleGrid cols={2}>
            <TextInput
              label="HSN Code"
              placeholder="e.g. 5208"
              value={prodHsn}
              onChange={(e) => setProdHsn(e.currentTarget.value)}
            />
            <Select
              label="Default Tax Rate"
              data={
                taxRates?.map((t: TaxRate) => ({
                  value: String(t.id),
                  label: `${t.name} (${t.ratePercent}%)`,
                })) || []
              }
              value={prodTaxId}
              onChange={(val) => setProdTaxId(val || '')}
            />
          </SimpleGrid>

          <SimpleGrid cols={2}>
            <NumberInput
              label="Default Sale Rate (₹)"
              min={0}
              value={prodSaleRate}
              onChange={(val) => setProdSaleRate(Number(val) || 0)}
            />
            <NumberInput
              label="Default Purchase Rate (₹)"
              min={0}
              value={prodPurchaseRate}
              onChange={(val) => setProdPurchaseRate(Number(val) || 0)}
            />
          </SimpleGrid>

          <Group justify="flex-end" mt="md">
            <Button variant="default" onClick={closeProdModal}>
              Cancel
            </Button>
            <Button
              color="indigo"
              onClick={() => saveProductMutation.mutate()}
              loading={saveProductMutation.isPending}
              disabled={!prodName.trim() || !prodUnitId}
            >
              Save Product
            </Button>
          </Group>
        </Stack>
      </Modal>

      {/* Unit Modal */}
      <Modal opened={unitModalOpened} onClose={closeUnitModal} title="Add Unit of Measure" size="sm">
        <Stack gap="sm">
          <TextInput
            label="Code (Symbol)"
            placeholder="e.g. mtr / kg / pcs"
            required
            value={unitCode}
            onChange={(e) => setUnitCode(e.currentTarget.value.toLowerCase())}
          />
          <TextInput
            label="Unit Name"
            placeholder="e.g. Metres"
            required
            value={unitName}
            onChange={(e) => setUnitName(e.currentTarget.value)}
          />
          <NumberInput
            label="Decimal Places (0-3)"
            min={0}
            max={3}
            value={unitDecimals}
            onChange={(val) => setUnitDecimals(Number(val) || 0)}
          />
          <Group justify="flex-end" mt="md">
            <Button variant="default" onClick={closeUnitModal}>
              Cancel
            </Button>
            <Button
              color="indigo"
              onClick={() => createUnitMutation.mutate()}
              loading={createUnitMutation.isPending}
              disabled={!unitCode.trim() || !unitName.trim()}
            >
              Save Unit
            </Button>
          </Group>
        </Stack>
      </Modal>

      {/* Tax Slab Modal */}
      <Modal opened={taxModalOpened} onClose={closeTaxModal} title="Add GST Tax Rate Slab" size="sm">
        <Stack gap="sm">
          <TextInput
            label="Tax Slab Name"
            placeholder="e.g. GST 5% / GST 12%"
            required
            value={taxName}
            onChange={(e) => setTaxName(e.currentTarget.value)}
          />
          <NumberInput
            label="Rate Percent (%)"
            min={0}
            max={100}
            value={taxPercent}
            onChange={(val) => setTaxPercent(Number(val) || 0)}
          />
          <Group justify="flex-end" mt="md">
            <Button variant="default" onClick={closeTaxModal}>
              Cancel
            </Button>
            <Button
              color="indigo"
              onClick={() => createTaxMutation.mutate()}
              loading={createTaxMutation.isPending}
              disabled={!taxName.trim()}
            >
              Save Slab
            </Button>
          </Group>
        </Stack>
      </Modal>

      {/* Expense Category Modal */}
      <Modal opened={catModalOpened} onClose={closeCatModal} title="Add Expense Category" size="sm">
        <Stack gap="sm">
          <TextInput
            label="Category Name"
            placeholder="e.g. Weaving Wages / Diesel & Fuel"
            required
            value={catName}
            onChange={(e) => setCatName(e.currentTarget.value)}
          />
          <Text size="xs" c="dimmed">
            An underlying postable EXPENSE ledger account will automatically be created in the Chart of Accounts.
          </Text>
          <Group justify="flex-end" mt="md">
            <Button variant="default" onClick={closeCatModal}>
              Cancel
            </Button>
            <Button
              color="indigo"
              onClick={() => createCatMutation.mutate()}
              loading={createCatMutation.isPending}
              disabled={!catName.trim()}
            >
              Save Category
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Container>
  );
};
