import 'react-native-gesture-handler';
import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import TodayScreen from './src/screens/TodayScreen';
import SundayRitualScreen from './src/screens/SundayRitualScreen';
import CreateTaskModal from './src/screens/CreateTaskModal';
import WeeklyRecapScreen from './src/screens/WeeklyRecapScreen';

const Stack = createStackNavigator();

export default function App() {
  return (
    <NavigationContainer>
      <Stack.Navigator
        initialRouteName="Home"
        screenOptions={{
          headerShown: false,
          cardStyle: { backgroundColor: '#FCFAF5' },
        }}
      >
        <Stack.Screen name="Home" component={TodayScreen} />
        <Stack.Screen name="SundayRitual" component={SundayRitualScreen} />
        <Stack.Screen 
          name="CreateTaskModal" 
          component={CreateTaskModal} 
          options={{
            presentation: 'transparentModal',
            cardStyle: { backgroundColor: 'transparent' },
          }}
        />
        <Stack.Screen name="WeeklyRecap" component={WeeklyRecapScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
