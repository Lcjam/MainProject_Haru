import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { ILocation } from '../../types/map';

interface MapState {
  myLocation: ILocation;
  yourLocation: ILocation;
  endLocation: ILocation;
}

const initialState: MapState = {
  myLocation: {
    lat: 0,
    lng: 0,
    address: '',
    meetingPlace: ''
  },
  yourLocation: {
    lat: 0,
    lng: 0,
    address: '',
    meetingPlace: ''
  },
  endLocation: {
    lat: 0,
    lng: 0,
    address: '',
    meetingPlace: ''
  }
};

const mapSlice = createSlice({
  name: 'map',
  initialState,
  reducers: {
    setMyLocation: (state, action: PayloadAction<ILocation>) => {
      state.myLocation = action.payload;
    },
    setYourLocation: (state, action: PayloadAction<ILocation>) => {
      state.yourLocation = action.payload;
    },
    setEndLocation: (state, action: PayloadAction<ILocation>) => {
      state.endLocation = action.payload;
    },
    resetLocations: (state) => {
      state.myLocation = initialState.myLocation;
      state.yourLocation = initialState.yourLocation;
      state.endLocation = initialState.endLocation;
    }
  }
});

export const { setMyLocation, setYourLocation, setEndLocation, resetLocations } = mapSlice.actions;
export default mapSlice.reducer;
