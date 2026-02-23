import React, { useEffect, useState } from "react";
import { View, Text, Button, ScrollView, StyleSheet } from "react-native";
import {
  initialize,
  requestPermission,
  readRecords,
} from "react-native-health-connect";

export default function HomeScreen() {
  const [heartRate, setHeartRate] = useState<any[]>([]);
  const [steps, setSteps] = useState<any[]>([]);
  const [calories, setCalories] = useState<any[]>([]);

  useEffect(() => {
    initHealth();
  }, []);

  const initHealth = async () => {
    await initialize();

    await requestPermission([
      { accessType: "read", recordType: "HeartRate" },
      { accessType: "read", recordType: "Steps" },
      { accessType: "read", recordType: "ActiveCaloriesBurned" },
      { accessType: "read", recordType: "ExerciseSession" },
    ]);
  };

  const loadLastMonth = async () => {
    const now = new Date();
    const lastMonth = new Date();
    lastMonth.setMonth(now.getMonth() - 1);

    const heart = await readRecords("HeartRate", {
      timeRangeFilter: {
        operator: "between",
        startTime: lastMonth.toISOString(),
        endTime: now.toISOString(),
      },
    });

    const stepData = await readRecords("Steps", {
      timeRangeFilter: {
        operator: "between",
        startTime: lastMonth.toISOString(),
        endTime: now.toISOString(),
      },
    });

    const calorieData = await readRecords("ActiveCaloriesBurned", {
      timeRangeFilter: {
        operator: "between",
        startTime: lastMonth.toISOString(),
        endTime: now.toISOString(),
      },
    });

    setHeartRate(heart.records);
    setSteps(stepData.records);
    setCalories(calorieData.records);
  };

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.title}>Health Dashboard</Text>

      <Button title="Load Last Month Data" onPress={loadLastMonth} />

      <View style={styles.card}>
        <Text>Heart Rate Records: {heartRate.length}</Text>
      </View>

      <View style={styles.card}>
        <Text>Steps Records: {steps.length}</Text>
      </View>

      <View style={styles.card}>
        <Text>Calories Records: {calories.length}</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
  },
  title: {
    fontSize: 22,
    fontWeight: "bold",
    marginBottom: 20,
  },
  card: {
    backgroundColor: "#eee",
    padding: 15,
    marginTop: 15,
    borderRadius: 10,
  },
});