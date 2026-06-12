import React from 'react';

interface NavigationItem {
  label: string;
  onClick: () => void;
}

interface NavigationBarProps {
  items: NavigationItem[];
}

const NavigationBar: React.FC<NavigationBarProps> = ({ items }) => {
  return (
    <nav className="w-full">
      <ul className="flex items-center gap-4">
        {items.map((item, index) => (
          <li key={index}>
            <label className="px-4 py-2 text-dark hover:text-primary-dark" onClick={item.onClick}>
              {item.label}
            </label>
          </li>
        ))}
      </ul>
    </nav>
  );
};

export default NavigationBar;
