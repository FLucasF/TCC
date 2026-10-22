package hotel.web;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import hotel.documents.Document;

public record DocumentResponse(@JsonUnwrapped Document document) {
}
