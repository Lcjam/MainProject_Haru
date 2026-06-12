interface GridItemProps {
  children: React.ReactNode;
  colSpan?: 1 | 2 | 3 | 4 | 5 | 6 | 12;
  rowSpan?: 1 | 2 | 3 | 4 | 5 | 6;
  className?: string;
}

const GridItem = ({ children, colSpan = 1, rowSpan = 1, className = '' }: GridItemProps) => {
  const itemClasses = [
    colSpan && `col-span-${colSpan}`,
    rowSpan && `row-span-${rowSpan}`,
    className
  ]
    .filter(Boolean)
    .join(' ');

  return <div className={itemClasses}>{children}</div>;
};

export default GridItem;
