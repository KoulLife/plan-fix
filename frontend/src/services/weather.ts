import {
  Cloud,
  CloudDrizzle,
  CloudFog,
  CloudLightning,
  CloudRain,
  CloudSnow,
  CloudSun,
  Sun,
  type LucideIcon,
} from "lucide-react";

import type { GangwonRegion } from "@/components/ui/gangwon-region-map";

export interface WeatherDayItem {
  date: string;
  day: string;
  low: number;
  high: number;
  rainProb: number;
  weatherCode: number;
  description: string;
  icon: LucideIcon;
  iconClass: string;
}

export interface RegionCoords {
  lat: number;
  lon: number;
}

export const gangwonRegionCoords: Record<GangwonRegion, RegionCoords> = {
  철원: { lat: 38.1468, lon: 127.3134 },
  화천: { lat: 38.1062, lon: 127.7082 },
  양구: { lat: 38.1098, lon: 127.9897 },
  고성: { lat: 38.3806, lon: 128.4678 },
  춘천: { lat: 37.8813, lon: 127.7298 },
  홍천: { lat: 37.6972, lon: 127.8887 },
  인제: { lat: 38.0697, lon: 128.1704 },
  속초: { lat: 38.2070, lon: 128.5918 },
  양양: { lat: 38.0754, lon: 128.6189 },
  원주: { lat: 37.3422, lon: 127.9202 },
  횡성: { lat: 37.4918, lon: 127.985 },
  평창: { lat: 37.3705, lon: 128.3902 },
  강릉: { lat: 37.7519, lon: 128.8761 },
  영월: { lat: 37.1836, lon: 128.4619 },
  정선: { lat: 37.3806, lon: 128.6608 },
  동해: { lat: 37.5247, lon: 129.1143 },
  태백: { lat: 37.1641, lon: 128.9856 },
  삼척: { lat: 37.4499, lon: 129.1653 },
};

// 강원도 전체 선택 시 강원도청 소재지인 춘천 좌표를 기본값으로 사용
export const DEFAULT_REGION_COORDS: RegionCoords = gangwonRegionCoords["춘천"];

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

export function getWeatherInfoByCode(wmoCode: number): {
  icon: LucideIcon;
  iconClass: string;
  description: string;
} {
  if (wmoCode === 0) {
    return {
      icon: Sun,
      iconClass: "fill-amber-400 text-amber-400",
      description: "맑음",
    };
  }

  if (wmoCode === 1 || wmoCode === 2) {
    return {
      icon: CloudSun,
      iconClass: "fill-amber-300/80 text-muted-foreground/40",
      description: "구름 조금",
    };
  }

  if (wmoCode === 3) {
    return {
      icon: Cloud,
      iconClass: "fill-muted-foreground/30 text-muted-foreground",
      description: "흐림",
    };
  }

  if (wmoCode === 45 || wmoCode === 48) {
    return {
      icon: CloudFog,
      iconClass: "fill-muted-foreground/20 text-muted-foreground",
      description: "안개",
    };
  }

  if (wmoCode >= 51 && wmoCode <= 57) {
    return {
      icon: CloudDrizzle,
      iconClass: "fill-blue-400/20 text-blue-400",
      description: "이슬비",
    };
  }

  if ((wmoCode >= 61 && wmoCode <= 67) || (wmoCode >= 80 && wmoCode <= 82)) {
    return {
      icon: CloudRain,
      iconClass: "fill-blue-500/20 text-blue-500",
      description: "비",
    };
  }

  if ((wmoCode >= 71 && wmoCode <= 77) || (wmoCode >= 85 && wmoCode <= 86)) {
    return {
      icon: CloudSnow,
      iconClass: "fill-sky-300/30 text-sky-400",
      description: "눈",
    };
  }

  if (wmoCode >= 95) {
    return {
      icon: CloudLightning,
      iconClass: "fill-amber-400/30 text-amber-500",
      description: "뇌우",
    };
  }

  return {
    icon: CloudSun,
    iconClass: "fill-amber-300/80 text-muted-foreground/40",
    description: "구름 조금",
  };
}

export interface OpenMeteoDailyResponse {
  daily: {
    time: string[];
    weather_code: number[];
    temperature_2m_max: number[];
    temperature_2m_min: number[];
    precipitation_probability_max: (number | null)[];
  };
}

export async function fetch5DayWeather(
  region: GangwonRegion | null,
): Promise<WeatherDayItem[]> {
  const coords = region ? gangwonRegionCoords[region] : DEFAULT_REGION_COORDS;
  const url = `https://api.open-meteo.com/v1/forecast?latitude=${coords.lat}&longitude=${coords.lon}&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=Asia%2FSeoul&forecast_days=5`;

  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`날씨 정보를 불러오는 데 실패했습니다 (HTTP ${response.status})`);
  }

  const data: OpenMeteoDailyResponse = await response.json();
  const {
    time,
    weather_code,
    temperature_2m_max,
    temperature_2m_min,
    precipitation_probability_max,
  } = data.daily;

  return time.map((dateStr, idx) => {
    const [year, month, day] = dateStr.split("-").map(Number);
    const dateObj = new Date(year, month - 1, day);
    const dayOfWeek = WEEKDAYS[dateObj.getDay()] ?? "";
    const formattedDate = `${String(month).padStart(2, "0")}.${String(day).padStart(2, "0")}`;

    const code = weather_code?.[idx] ?? 0;
    const weatherInfo = getWeatherInfoByCode(code);
    const low = Math.round(temperature_2m_min?.[idx] ?? 0);
    const high = Math.round(temperature_2m_max?.[idx] ?? 0);
    const rainProb = Math.round(precipitation_probability_max?.[idx] ?? 0);

    return {
      date: formattedDate,
      day: dayOfWeek,
      low,
      high,
      rainProb,
      weatherCode: code,
      description: weatherInfo.description,
      icon: weatherInfo.icon,
      iconClass: weatherInfo.iconClass,
    };
  });
}
