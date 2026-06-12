import React from 'react';
import Card from './Card';

interface CardListProps {
  items: { title: string; description?: string; image?: string }[];
}

const CardList: React.FC<CardListProps> = ({ items }) => {
  return (
    <div className="grid grid-cols-4 gap-6 p-4">
      {items.map((item, index) => (
        <Card key={index} title={item.title} description={item.description} image={item.image} />
      ))}
    </div>
  );
};

export default CardList;
