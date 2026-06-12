import { useEffect, useRef, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css'; // Leaflet 기본 CSS
import './Leaflet.css'; // 커스텀 CSS
import { RootState } from '../../../store/store';
import { setEndLocation, setMyLocation, setYourLocation } from '../../../store/slices/mapSlice';
import { getAddressFromCoord } from '../../../services/api/productAPI';

interface OpenStreetMapProps {
  children?: React.ReactNode;
  nonClickable?: boolean;
  className?: string;
  mode?: 'myLocation' | 'yourLocation' | 'endLocation' | 'all' | 'myAndEnd';
}

const OpenMap: React.FC<OpenStreetMapProps> = ({ nonClickable = false, className, mode = 'endLocation' }) => {
  const dispatch = useDispatch();
  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const [markers, setMarkers] = useState<L.Marker[]>([]);
  const { myLocation, yourLocation, endLocation } = useSelector((state: RootState) => state.map);

  // 초기 지도 생성
  useEffect(() => {
    if (mapRef.current && !mapInstanceRef.current) {
      const defaultLocation: [number, number] = [37.5665, 126.978]; // 서울 기본 위치
      const newMap = L.map(mapRef.current).setView(defaultLocation, 13);

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(newMap);

      // 지도 클릭 이벤트 처리
      newMap.on('click', async (e) => {
        if (nonClickable) {
          console.log('클릭방지 모드입니다.');
          return;
        }
        const { lat, lng } = e.latlng;
        
        try {
          const location = await getAddressFromCoord(lat, lng);
          if (!location) {
            throw new Error('주소를 가져올 수 없습니다.');
          }

          const locationData = {
            lat,
            lng,
            address: location.address,
            meetingPlace: location.name
          };
          
          switch (mode) {
            case 'myLocation':
              dispatch(setMyLocation(locationData));
              break;
            case 'yourLocation':
              dispatch(setYourLocation(locationData));
              break;
            case 'endLocation':
              dispatch(setEndLocation(locationData));
              break;
            case 'myAndEnd':            
              dispatch(setEndLocation(locationData));
              break;            
          }
        } catch (error) {
          console.error('주소 변환 중 오류 발생:', error);
          const locationData = {
            lat,
            lng,
            address: '주소를 가져올 수 없습니다.',
            meetingPlace: '위치 정보 없음'
          };
          
          switch (mode) {
            case 'myLocation':
              dispatch(setMyLocation(locationData));
              break;
            case 'yourLocation':
              dispatch(setYourLocation(locationData));
              break;
            case 'endLocation':
              dispatch(setEndLocation(locationData));
              break;
            case 'myAndEnd':
              dispatch(setEndLocation(locationData));
              break;
          }
        }
      });

      mapInstanceRef.current = newMap;
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, []);

  // 마커 업데이트
  useEffect(() => {
    if (!mapInstanceRef.current) return;

    // 기존 마커 제거
    markers.forEach((marker) => marker.remove());
    const newMarkers: L.Marker[] = [];

    const addMarker = (location: { lat: number; lng: number }, type: 'myLocation' | 'yourLocation' | 'endLocation') => {
      if (!location?.lat || !location?.lng) return;

      const iconColors = {
        myLocation: '#3B82F6',
        yourLocation: '#EF4444',
        endLocation: '#10B981'
      };

      const popupTexts = {
        myLocation: '내 위치',
        yourLocation: '상대방 위치',
        endLocation: '목적지'
      };

      const icon = L.divIcon({
        className: 'custom-marker',
        html: `<div class="marker-content" style="background-color: ${iconColors[type]};"></div>`,
        iconSize: [30, 30],
        iconAnchor: [15, 15]
      });

      const marker = L.marker([location.lat, location.lng], { icon })
        .addTo(mapInstanceRef.current!)
        .bindPopup(popupTexts[type]);
      
      newMarkers.push(marker);
    };

    // 각 위치에 대한 마커 추가
    if (myLocation?.lat && myLocation?.lng && (mode === 'myLocation' || mode === 'all' || mode === 'myAndEnd')) {
      addMarker(myLocation, 'myLocation');
    }
    if (yourLocation?.lat && yourLocation?.lng && (mode === 'yourLocation' || mode === 'all')) {
      addMarker(yourLocation, 'yourLocation');
    }
    if (endLocation?.lat && endLocation?.lng && (mode === 'endLocation' || mode === 'all' || mode === 'myAndEnd')) {
      addMarker(endLocation, 'endLocation');
    }

    // 마지막으로 추가된 마커의 위치로 지도 중심 이동
    const lastMarker = newMarkers[newMarkers.length - 1];
    if (lastMarker) {
      mapInstanceRef.current.setView(lastMarker.getLatLng(), 13);
    }

    setMarkers(newMarkers);
  }, [myLocation?.lat, myLocation?.lng, yourLocation?.lat, yourLocation?.lng, endLocation?.lat, endLocation?.lng, mode]);

  return <div ref={mapRef} className={`w-full h-full ${className}`} />;
};

export default OpenMap;