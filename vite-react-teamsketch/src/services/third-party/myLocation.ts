interface IGeolocation {
  lat: number;
  lng: number;
  address: string;
}

// 좌표로 주소 가져오기
export const getAddressFromCoords = async (lat: number, lng: number): Promise<string> => {
  try {
    const response = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&addressdetails=1&accept-language=ko`,
      {
        headers: {
          'User-Agent': 'TeamSketch Application' // OpenStreetMap 권장사항
        }
      }
    );
    const data = await response.json();
    return data.display_name;
  } catch (error) {
    throw new Error(`주소를 가져오는데 실패했습니다: ${error}`);
  }
};

// 현재 위치 및 주소 가져오기
export const getCurrentLocation = async (): Promise<IGeolocation> => {
  if (!navigator.geolocation) {
    throw new Error('위치 정보가 지원되지 않는 브라우저입니다.');
  }

  try {
    const position = await new Promise<GeolocationPosition>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 10000, // 10초
        maximumAge: 30000 // 30초
      });
    });

    const lat = position.coords.latitude;
    const lng = position.coords.longitude;
    const address = await getAddressFromCoords(lat, lng);

    return {
      lat,
      lng,
      address
    };
  } catch (error) {
    throw new Error(`현재 위치를 가져오는데 실패했습니다: ${error}`);
  }
};

// 현재 위치 주변 랜덤 위치 생성 (반경 1km 이내)
export const getNearbyRandomLocation = (lat: number, lng: number): { lat: number; lng: number } => {
  // 1km 반경 내의 랜덤 위치 계산
  const radius = 0.01; // 약 1km
  const random_angle = Math.random() * Math.PI * 2;
  const random_radius = Math.sqrt(Math.random()) * radius;

  return {
    lat: lat + random_radius * Math.cos(random_angle),
    lng: lng + random_radius * Math.sin(random_angle)
  };
};

// 실시간 위치 추적 (30초마다 업데이트)
export const watchCurrentLocation = (
  onSuccess: (position: IGeolocation) => void,
  onError: (error: string) => void
) => {
  if (!navigator.geolocation) {
    onError('위치 정보가 지원되지 않는 브라우저입니다.');
    return;
  }

  const watchId = navigator.geolocation.watchPosition(
    async (position) => {
      try {
        const lat = position.coords.latitude;
        const lng = position.coords.longitude;
        const address = await getAddressFromCoords(lat, lng);
        onSuccess({ lat, lng, address });
      } catch (error) {
        onError(`위치 정보 조회 실패: ${error}`);
      }
    },
    (error) => onError(error.message),
    {
      enableHighAccuracy: true,
      timeout: 10000, // 10초 타임아웃
      maximumAge: 30000 // 30초마다 업데이트
    }
  );

  return () => navigator.geolocation.clearWatch(watchId);
};
