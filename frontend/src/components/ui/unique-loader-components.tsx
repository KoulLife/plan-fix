"use client";

import { motion } from "motion/react";

import { cn } from "@/lib/utils";

const loopTransition = (delay: number, duration = 1) => ({
  duration,
  repeat: Infinity,
  repeatType: "loop" as const,
  delay,
  ease: "easeInOut" as const,
});

type LoaderOneProps = {
  variant?: "default" | "inverse";
};

export function LoaderOne({ variant = "default" }: LoaderOneProps) {
  return (
    <div className="flex items-center gap-2" role="status" aria-label="로딩 중">
      {[0, 1, 2].map((index) => (
        <motion.div
          key={index}
          initial={{ y: 0 }}
          animate={{ y: [0, 10, 0] }}
          transition={loopTransition(index * 0.2)}
          className={cn(
            "rounded-full border",
            variant === "default" &&
              "h-4 w-4 border-primary/20 bg-gradient-to-b from-primary to-primary/55",
            variant === "inverse" &&
              "h-2.5 w-2.5 border-primary-foreground/40 bg-primary-foreground",
          )}
        />
      ))}
    </div>
  );
}

export function LoaderTwo() {
  const dots = [
    { delay: 0, className: "" },
    { delay: 0.4, className: "-translate-x-2" },
    { delay: 0.8, className: "-translate-x-4" },
  ];

  return (
    <div className="flex items-center" role="status" aria-label="로딩 중">
      {dots.map((dot) => (
        <motion.div
          key={dot.delay}
          initial={{ x: 0 }}
          animate={{ x: [0, 20, 0] }}
          transition={loopTransition(dot.delay * 0.2, 2)}
          className={`${dot.className} h-4 w-4 rounded-full bg-muted-foreground/30 shadow-md ring-1 ring-primary/10 dark:bg-muted-foreground/60`}
        />
      ))}
    </div>
  );
}

export function LoaderThree() {
  return (
    <motion.svg
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="h-20 w-20 stroke-muted-foreground [--loader-fill-final:hsl(var(--primary))] [--loader-fill-initial:hsl(var(--muted))] dark:stroke-foreground"
      role="status"
      aria-label="로딩 중"
    >
      <motion.path stroke="none" d="M0 0h24v24H0z" fill="none" />
      <motion.path
        initial={{ pathLength: 0, fill: "var(--loader-fill-initial)" }}
        animate={{ pathLength: 1, fill: "var(--loader-fill-final)" }}
        transition={{
          duration: 2,
          ease: "easeInOut",
          repeat: Infinity,
          repeatType: "reverse",
        }}
        d="M13 3l0 7l6 0l-8 11l0 -7l-6 0l8 -11"
      />
    </motion.svg>
  );
}

export function LoaderFour({ text = "불러오는 중..." }: { text?: string }) {
  return (
    <div
      className="relative font-bold text-foreground [perspective:1000px]"
      role="status"
      aria-label={text}
    >
      <motion.span
        animate={{
          skewX: [0, -40, 0],
          scaleX: [1, 2, 1],
        }}
        transition={{
          duration: 0.05,
          repeat: Infinity,
          repeatType: "reverse",
          repeatDelay: 2,
          ease: "linear",
          times: [0, 0.2, 0.5, 0.8, 1],
        }}
        className="relative z-20 inline-block"
      >
        {text}
      </motion.span>
      <motion.span
        className="absolute inset-0 text-primary/50 blur-[0.5px]"
        animate={{
          x: [-2, 4, -3, 1.5, -2],
          y: [-2, 4, -3, 1.5, -2],
          opacity: [0.3, 0.9, 0.4, 0.8, 0.3],
        }}
        transition={{
          duration: 0.5,
          repeat: Infinity,
          repeatType: "reverse",
          ease: "linear",
          times: [0, 0.2, 0.5, 0.8, 1],
        }}
        aria-hidden="true"
      >
        {text}
      </motion.span>
      <motion.span
        className="absolute inset-0 text-foreground/30"
        animate={{
          x: [0, 1, -1.5, 1.5, -1, 0],
          y: [0, -1, 1.5, -0.5, 0],
          opacity: [0.4, 0.8, 0.3, 0.9, 0.4],
        }}
        transition={{
          duration: 0.8,
          repeat: Infinity,
          repeatType: "reverse",
          ease: "linear",
          times: [0, 0.3, 0.6, 0.8, 1],
        }}
        aria-hidden="true"
      >
        {text}
      </motion.span>
    </div>
  );
}

export function LoaderFive({ text }: { text: string }) {
  return (
    <div
      className="font-bold [--loader-shadow:hsl(var(--primary))]"
      role="status"
      aria-label={text}
    >
      {text.split("").map((char, index) => (
        <motion.span
          key={`${char}-${index}`}
          className="inline-block"
          initial={{ scale: 1, opacity: 0.5 }}
          animate={{
            scale: [1, 1.1, 1],
            textShadow: [
              "0 0 0 var(--loader-shadow)",
              "0 0 1px var(--loader-shadow)",
              "0 0 0 var(--loader-shadow)",
            ],
            opacity: [0.5, 1, 0.5],
          }}
          transition={{
            duration: 0.5,
            repeat: Infinity,
            repeatType: "loop",
            delay: index * 0.05,
            ease: "easeInOut",
            repeatDelay: 2,
          }}
          aria-hidden="true"
        >
          {char === " " ? "\u00A0" : char}
        </motion.span>
      ))}
    </div>
  );
}
