const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..");
const storyboardPath = path.join(__dirname, "agent-platform-storyboard.js");
const outputPath = path.join(root, "chatgpt.pen");

const source = fs.readFileSync(storyboardPath, "utf8");
const render = new Function("pencil", source);
const width = 3140;
const height = 2580;
const generated = render({ width, height });

const doc = {
  version: "2.11",
  children: [
    {
      type: "frame",
      id: "storyboardRoot",
      name: "Agent Platform Storyboard",
      x: 64,
      y: 64,
      width,
      height,
      layout: "none",
      fill: "#F3F6FB",
      children: generated,
    },
  ],
};

fs.writeFileSync(outputPath, JSON.stringify(doc, null, 2));
console.log(`Wrote ${outputPath}`);
