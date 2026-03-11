import { useState, useRef, useEffect } from "react";
import { createPortal } from "react-dom";
import type { ArmyUnit, Datasheet } from "../types";
import styles from "../pages/UnitRow.module.css";

interface LeaderOption {
  unit: ArmyUnit;
  index: number;
  datasheet: Datasheet;
}

interface Props {
  attachedLeader: LeaderOption | null;
  availableLeaders: LeaderOption[];
  onAttach: (leaderIndex: number) => void;
  onDetach: (leaderIndex: number) => void;
}

export function LeaderSlot({ attachedLeader, availableLeaders, onAttach, onDetach }: Props) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [pickerPos, setPickerPos] = useState({ top: 0, left: 0 });
  const containerRef = useRef<HTMLDivElement>(null);
  const pickerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isExpanded) return;
    const handleClickOutside = (e: MouseEvent) => {
      const target = e.target as Node;
      const inContainer = containerRef.current?.contains(target) ?? false;
      const inPicker = pickerRef.current?.contains(target) ?? false;
      if (!inContainer && !inPicker) {
        setIsExpanded(false);
      }
    };
    document.addEventListener("click", handleClickOutside);
    return () => document.removeEventListener("click", handleClickOutside);
  }, [isExpanded]);

  const handleButtonClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    const rect = e.currentTarget.getBoundingClientRect();
    setPickerPos({ top: rect.bottom + 4, left: rect.left });
    setIsExpanded(!isExpanded);
  };

  const handleSelect = (leaderIndex: number) => {
    onAttach(leaderIndex);
    setIsExpanded(false);
  };

  const handleRemove = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (attachedLeader) {
      onDetach(attachedLeader.index);
    }
  };

  if (attachedLeader) {
    return (
      <div className={`${styles.leaderSlot} ${styles.leaderSlotFilled}`} ref={containerRef}>
        <span className={styles.leaderSlotName}>{attachedLeader.datasheet.name}</span>
        <button
          type="button"
          className={styles.leaderSlotRemove}
          onClick={handleRemove}
          title="Remove leader"
          aria-label="Remove leader"
        >
          ×
        </button>
      </div>
    );
  }

  return (
    <div className={`${styles.leaderSlot} ${styles.leaderSlotEmpty}`} ref={containerRef}>
      <button
        type="button"
        className={styles.leaderSlotAdd}
        onClick={handleButtonClick}
      >
        + Add Leader
      </button>
      {isExpanded && createPortal(
        <div ref={pickerRef} className={styles.leaderPicker} style={{ top: pickerPos.top, left: pickerPos.left }}>
          {availableLeaders.length > 0 ? (
            availableLeaders.map((leader) => (
              <div
                key={leader.index}
                className={styles.leaderPickerOption}
                onClick={() => handleSelect(leader.index)}
              >
                {leader.datasheet.name}
              </div>
            ))
          ) : (
            <div className={styles.leaderPickerEmpty}>No available leaders</div>
          )}
        </div>,
        document.body
      )}
    </div>
  );
}
