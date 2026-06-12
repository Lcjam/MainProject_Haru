import { useState, useRef } from 'react';

const TestFunc = () => {
  const [test, setTest] = useState(0);
  const [test2, setTest2] = useState(0);
  const testRef = useRef(0);
  const handleTest = () => {
    setTest(test + 1);
  };

  const handleRef = () => {
    testRef.current++;
  };
  return (
    <div>
      TestFunc
      <button onClick={handleTest}>test</button>
      {test}
      <button onClick={() => setTest2(test2 + 1)}>test2</button>
      {test2}
      <button onClick={handleRef}>testRef</button>
      {testRef.current}
    </div>
  );
};

export default TestFunc;
