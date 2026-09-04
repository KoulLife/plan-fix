import { Node, mergeAttributes } from "@tiptap/core";

export interface SpotCardAttributes {
  spotId: number | string | null;
  title: string;
  address: string;
  category?: string;
  imageUrl?: string;
}

declare module "@tiptap/core" {
  interface Commands<ReturnType> {
    spotCard: {
      insertSpotCard: (attributes: SpotCardAttributes) => ReturnType;
    };
  }
}

export const SpotCardExtension = Node.create({
  name: "spotCard",
  group: "block",
  atom: true,
  draggable: true,

  addAttributes() {
    return {
      spotId: {
        default: null,
        parseHTML: (element) => element.getAttribute("data-spot-id"),
        renderHTML: (attributes) => ({
          "data-spot-id": attributes.spotId,
        }),
      },
      title: {
        default: "",
        parseHTML: (element) => element.getAttribute("data-spot-title") || "",
        renderHTML: (attributes) => ({
          "data-spot-title": attributes.title,
        }),
      },
      address: {
        default: "",
        parseHTML: (element) => element.getAttribute("data-spot-address") || "",
        renderHTML: (attributes) => ({
          "data-spot-address": attributes.address,
        }),
      },
      category: {
        default: "여행지",
        parseHTML: (element) => element.getAttribute("data-spot-category") || "여행지",
        renderHTML: (attributes) => ({
          "data-spot-category": attributes.category,
        }),
      },
      imageUrl: {
        default: "",
        parseHTML: (element) => element.getAttribute("data-spot-image") || "",
        renderHTML: (attributes) => ({
          "data-spot-image": attributes.imageUrl,
        }),
      },
    };
  },

  parseHTML() {
    return [
      {
        tag: "div[data-spot-card]",
      },
    ];
  },

  renderHTML({ HTMLAttributes }) {
    const title = HTMLAttributes["data-spot-title"] || "장소명";
    const address = HTMLAttributes["data-spot-address"] || "주소 정보 없음";
    const category = HTMLAttributes["data-spot-category"] || "여행지";
    const imageUrl = HTMLAttributes["data-spot-image"];

    const innerElements: unknown[] = [
      imageUrl
        ? [
            "div",
            { class: "h-16 w-16 shrink-0 overflow-hidden rounded-lg bg-muted border border-border/50" },
            ["img", { src: imageUrl, alt: title, class: "h-full w-full object-cover" }],
          ]
        : [
            "div",
            { class: "flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary text-xl" },
            "📍",
          ],
      [
        "div",
        { class: "min-w-0 flex-1" },
        [
          "div",
          { class: "flex items-center gap-2" },
          ["span", { class: "rounded-md bg-primary/15 px-2 py-0.5 text-xs font-semibold text-primary" }, category],
          ["h4", { class: "truncate text-base font-bold text-foreground" }, title],
        ],
        ["p", { class: "mt-1 truncate text-xs text-muted-foreground" }, address],
      ],
    ];

    return [
      "div",
      mergeAttributes(HTMLAttributes, {
        "data-spot-card": "true",
        class:
          "travel-spot-card my-6 flex items-center gap-4 rounded-2xl border border-primary/25 bg-gradient-to-r from-primary/5 via-background to-muted/20 p-4 shadow-sm select-none",
      }),
      ...innerElements,
    ];
  },

  addCommands() {
    return {
      insertSpotCard:
        (attributes: SpotCardAttributes) =>
        ({ commands }) => {
          return commands.insertContent([
            {
              type: this.name,
              attrs: attributes,
            },
            {
              type: "paragraph",
            },
          ]);
        },
    };
  },
});
