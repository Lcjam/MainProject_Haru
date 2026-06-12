interface GridProps {
  children: React.ReactNode;
  cols?: 1 | 2 | 3 | 4 | 5 | 6 | 12;
  rows?: 1 | 2 | 3 | 4 | 5 | 6;
  gap?: 'none' | 'xs' | 'sm' | 'md' | 'lg';
  className?: string;
}

const Grid = ({ children, cols = 12, rows, gap = 'md', className = '' }: GridProps) => {
  return (
    <div
      className={`
      grid 
      grid-cols-${cols} 
      ${rows ? `grid-rows-${rows}` : ''} 
      gap-${gap}
      ${className}
    `
        .trim()
        .replace(/\s+/g, ' ')}
    >
      {children}
    </div>
  );
};

export default Grid;
