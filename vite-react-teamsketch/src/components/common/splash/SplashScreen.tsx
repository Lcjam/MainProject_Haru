import React, { useEffect, useState } from 'react';
import logo192 from '../../../assets/images/logo192.png';
import '../../../assets/styles/splash.css';

const SplashScreen: React.FC = () => {
  const [isVisible, setIsVisible] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsVisible(false);
    }, 2500); // 2.5초 후에 스플래시 스크린 숨김

    return () => clearTimeout(timer);
  }, []);

  if (!isVisible) return null;

  return (
    <div className="splash-screen">
      <img src={logo192} alt="Haru Logo" className="splash-logo" />
    </div>
  );
};

export default SplashScreen; 