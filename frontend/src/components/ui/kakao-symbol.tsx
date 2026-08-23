import { type SVGProps } from "react";

export default function KakaoSymbol(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
      {...props}
    >
      <path
        fill="currentColor"
        d="M12 3C6.48 3 2 6.57 2 10.98c0 2.84 1.86 5.33 4.66 6.75l-1.18 4.33a.48.48 0 0 0 .73.53l5.16-3.4c.21.01.42.02.63.02 5.52 0 10-3.58 10-8.23C22 6.57 17.52 3 12 3Z"
      />
    </svg>
  );
}
