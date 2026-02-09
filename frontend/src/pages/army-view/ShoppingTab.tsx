import styles from "../ArmyViewPage.module.css";

interface ShoppingItem {
  datasheetId: string;
  name: string;
  needed: number;
  owned: number;
  missing: number;
}

interface Props {
  shoppingList: ShoppingItem[];
}

export function ShoppingTab({ shoppingList }: Props) {
  return (
    <div>
      <p className={styles.meta} style={{ marginBottom: 16 }}>
        {shoppingList.filter((s) => s.missing > 0).length === 0
          ? "You own all units needed for this army!"
          : `${shoppingList.filter((s) => s.missing > 0).length} unit(s) need to be acquired`}
      </p>
      <div className={styles.grid}>
        {shoppingList.map((item) => (
          <div
            key={item.datasheetId}
            className={`${styles.shoppingItem} ${item.missing > 0 ? styles.shoppingMissing : styles.shoppingOwned}`}
          >
            <span className={styles.shoppingName}>{item.name}</span>
            <div className={styles.shoppingDetails}>
              <span className={styles.shoppingBadge}>
                Need: {item.needed}
              </span>
              <span className={styles.shoppingBadge}>
                Own: {item.owned}
              </span>
              {item.missing > 0 && (
                <span className={`${styles.shoppingBadge} ${styles.shoppingMissingBadge}`}>
                  Missing: {item.missing}
                </span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
