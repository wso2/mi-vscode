import { TextDocument } from "vscode-languageserver-textdocument";
import { XMLDocument } from "./parser/xmlNode.js";
import { parseXMLDocument } from "./parser/xmlParser.js";
import { Position } from "./utils/positionUtils.js";

export function toXMLDocument(document: TextDocument): XMLDocument {
  return parseXMLDocument(document.uri, document.getText());
}

export function toXMLPosition(position: Position): Position {
  return position;
}
