import { fetch5DayWeather, getWeatherInfoByCode } from "@/services/weather";

describe("weather service", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("getWeatherInfoByCode", () => {
    test("returns correct icon and description for sunny weather (code 0)", () => {
      const info = getWeatherInfoByCode(0);
      expect(info.description).toBe("맑음");
      expect(info.iconClass).toContain("fill-amber-400");
    });

    test("returns correct icon and description for rain (code 61)", () => {
      const info = getWeatherInfoByCode(61);
      expect(info.description).toBe("비");
      expect(info.iconClass).toContain("fill-blue-500");
    });

    test("returns correct icon and description for snow (code 71)", () => {
      const info = getWeatherInfoByCode(71);
      expect(info.description).toBe("눈");
      expect(info.iconClass).toContain("fill-sky-300");
    });
  });

  describe("fetch5DayWeather", () => {
    test("fetches 5-day weather data and formats it correctly", async () => {
      const mockApiResponse = {
        daily: {
          time: ["2026-09-02", "2026-09-03", "2026-09-04", "2026-09-05", "2026-09-06"],
          weather_code: [0, 1, 3, 61, 71],
          temperature_2m_max: [28.2, 27.5, 26.1, 24.3, 22.0],
          temperature_2m_min: [19.8, 18.5, 17.2, 16.5, 15.0],
          precipitation_probability_max: [0, 10, 30, 80, 50],
        },
      };

      global.fetch = jest.fn().mockResolvedValue({
        ok: true,
        json: async () => mockApiResponse,
      }) as jest.Mock;

      const result = await fetch5DayWeather("강릉");

      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining("latitude=37.7519&longitude=128.8761"),
      );
      expect(result).toHaveLength(5);
      expect(result[0]).toEqual(
        expect.objectContaining({
          date: "09.02",
          day: "수",
          high: 28,
          low: 20,
          rainProb: 0,
          description: "맑음",
        }),
      );
      expect(result[3]).toEqual(
        expect.objectContaining({
          date: "09.05",
          day: "토",
          high: 24,
          low: 17,
          rainProb: 80,
          description: "비",
        }),
      );
    });

    test("throws error when API response is not ok", async () => {
      global.fetch = jest.fn().mockResolvedValue({
        ok: false,
        status: 500,
      }) as jest.Mock;

      await expect(fetch5DayWeather(null)).rejects.toThrow("날씨 정보를 불러오는 데 실패했습니다");
    });
  });
});
