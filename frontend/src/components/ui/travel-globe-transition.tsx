"use client";

import { motion, useReducedMotion } from "motion/react";

const continents = [
  "M15 58 34 42 57 45 73 59 67 76 51 79 45 94 27 87 17 71Z",
  "M50 95 71 102 77 122 67 147 58 172 48 151 43 128 36 109Z",
  "M92 55 110 40 137 37 151 45 173 42 192 55 202 68 190 78 169 73 157 84 138 80 124 93 110 88 102 75 88 69Z",
  "M118 90 143 84 158 98 153 123 139 151 124 139 116 113 105 99Z",
  "M182 136 203 132 215 145 207 160 188 163 177 150Z",
  "M171 91 178 86 184 91 179 98Z",
];

export default function TravelGlobeTransition() {
  const shouldReduceMotion = useReducedMotion();

  return (
    <motion.main
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.18 }}
      className="fixed inset-0 z-[100] flex min-h-screen items-center justify-center bg-background px-5 sm:px-8 lg:px-10"
      role="status"
      aria-live="polite"
      aria-label="메인 화면을 준비하는 중"
    >
      <div className="w-full max-w-xl rounded-lg bg-foreground px-6 py-10 text-center shadow-[0_28px_80px_hsl(var(--foreground)/0.28)] ring-1 ring-primary/20 sm:px-12 sm:py-12">
        <div
          className="relative mx-auto aspect-square w-[clamp(16rem,32vmin,24rem)] max-w-full"
          aria-hidden="true"
        >
          <div className="absolute inset-3 rounded-full bg-primary/35 blur-3xl" />
          <svg className="relative h-full w-full" viewBox="0 0 240 240">
            <defs>
              <clipPath id="travel-globe-clip">
                <circle cx="120" cy="120" r="106" />
              </clipPath>
              <radialGradient id="travel-globe-shade" cx="34%" cy="28%" r="76%">
                <stop offset="0%" stopColor="hsl(var(--primary) / 0.96)" />
                <stop offset="58%" stopColor="hsl(var(--primary) / 0.58)" />
                <stop offset="100%" stopColor="hsl(var(--foreground))" />
              </radialGradient>
            </defs>

            <circle cx="120" cy="120" r="108" fill="url(#travel-globe-shade)" />

            <g clipPath="url(#travel-globe-clip)">
              <motion.g
                initial={{ x: 0 }}
                animate={{ x: shouldReduceMotion ? 0 : -240 }}
                transition={{ duration: 2.4, repeat: Infinity, ease: "linear" }}
                fill="hsl(var(--primary-foreground))"
                fillOpacity="0.68"
                stroke="hsl(var(--primary-foreground))"
                strokeOpacity="0.3"
                strokeWidth="1"
              >
                {[-240, 0, 240].map((offset) => (
                  <g key={offset} transform={`translate(${offset} 0)`}>
                    {continents.map((path) => (
                      <path key={path} d={path} />
                    ))}
                  </g>
                ))}
              </motion.g>

              <g
                fill="none"
                stroke="hsl(var(--primary-foreground))"
                strokeOpacity="0.34"
                strokeWidth="1"
              >
                <ellipse cx="120" cy="120" rx="78" ry="106" />
                <ellipse cx="120" cy="120" rx="42" ry="106" />
                <ellipse cx="120" cy="120" rx="106" ry="72" />
                <ellipse cx="120" cy="120" rx="106" ry="36" />
                <path d="M14 120h212" />
              </g>

              <motion.path
                d="M28 49C74 18 166 18 212 49"
                fill="none"
                stroke="hsl(var(--primary-foreground))"
                strokeWidth="2"
                strokeOpacity="0.46"
                initial={{ pathLength: 0.25, pathOffset: 0 }}
                animate={{ pathOffset: shouldReduceMotion ? 0 : 1 }}
                transition={{ duration: 1.2, repeat: Infinity, ease: "linear" }}
              />
            </g>

            <circle
              cx="120"
              cy="120"
              r="107"
              fill="none"
              stroke="hsl(var(--primary-foreground))"
              strokeOpacity="0.72"
              strokeWidth="1.5"
            />
          </svg>
        </div>

        <p className="mt-4 text-sm font-semibold tracking-[0.28em] text-primary">PLANFIX</p>
        <p className="mt-2 text-sm text-background/60">여행을 준비하고 있어요</p>
      </div>
    </motion.main>
  );
}
