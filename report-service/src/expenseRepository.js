// Read-only access to the collection that expense-api writes.
export function createExpenseRepository(db) {
  const expenses = db.collection('expenses');

  return {
    async findByUserBetween(userId, from, to) {
      const docs = await expenses
        .find(
          { userId, date: { $gte: from, $lt: to } },
          { projection: { amount: 1, category: 1, date: 1, description: 1, paymentMethod: 1, tags: 1 } },
        )
        .sort({ date: 1 })
        .toArray();

      // amount is a Decimal128 in Mongo
      return docs.map((d) => ({ ...d, amount: Number(d.amount.toString()) }));
    },
  };
}
