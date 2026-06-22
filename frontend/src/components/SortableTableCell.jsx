import { TableCell, TableSortLabel } from "@mui/material";

export function nextSortState(currentSort, field) {
  if (currentSort.field !== field) {
    return { field, direction: "asc" };
  }

  return { field, direction: currentSort.direction === "asc" ? "desc" : "asc" };
}

function getValue(row, field, valueGetters) {
  const getter = valueGetters[field];
  if (getter) {
    return getter(row);
  }

  return field.split(".").reduce((value, key) => value?.[key], row);
}

export function sortRows(rows, sort, valueGetters = {}) {
  return [...rows].sort((left, right) => {
    const leftValue = getValue(left, sort.field, valueGetters);
    const rightValue = getValue(right, sort.field, valueGetters);
    const direction = sort.direction === "asc" ? 1 : -1;

    if (leftValue === rightValue) {
      return 0;
    }

    if (leftValue === null || leftValue === undefined) {
      return 1 * direction;
    }

    if (rightValue === null || rightValue === undefined) {
      return -1 * direction;
    }

    if (typeof leftValue === "number" && typeof rightValue === "number") {
      return (leftValue - rightValue) * direction;
    }

    if (typeof leftValue === "boolean" && typeof rightValue === "boolean") {
      return (Number(leftValue) - Number(rightValue)) * direction;
    }

    const leftText = String(leftValue);
    const rightText = String(rightValue);
    const looksLikeDate = /^\d{4}-\d{2}-\d{2}/;
    if (looksLikeDate.test(leftText) && looksLikeDate.test(rightText)) {
      const leftDate = Date.parse(leftText);
      const rightDate = Date.parse(rightText);
      if (!Number.isNaN(leftDate) && !Number.isNaN(rightDate)) {
        return (leftDate - rightDate) * direction;
      }
    }

    return leftText.localeCompare(rightText, undefined, {
      numeric: true,
      sensitivity: "base",
    }) * direction;
  });
}

export function SortableTableCell({ sort, field, onSort, children, ...props }) {
  const active = sort.field === field;

  return (
    <TableCell {...props} sortDirection={active ? sort.direction : false}>
      <TableSortLabel
        active={active}
        direction={active ? sort.direction : "asc"}
        onClick={() => onSort(field)}
      >
        {children}
      </TableSortLabel>
    </TableCell>
  );
}
