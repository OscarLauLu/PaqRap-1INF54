import React from 'react';

export const PantallaPendiente: React.FC<{ titulo: string; detalle: string }> = ({ titulo, detalle }) => (
  <main className="flex-1 p-6 md:p-8">
    <div className="bg-white rounded-xl border border-dashed border-gray-300 p-12 text-center">
      <h2 className="text-lg font-bold text-gray-700">{titulo}</h2>
      <p className="text-sm text-gray-400 mt-1">{detalle}</p>
    </div>
  </main>
);
