import { createContext, useContext, useMemo, useState } from "react";
import { codeMessages, messages } from "./messages";

const LANGUAGE_STORAGE_KEY = "railway-ticket-system-language";
const DEFAULT_LANGUAGE = "en";
const supportedLanguages = ["en", "uk"];

const I18nContext = createContext(null);

function loadLanguage() {
  const stored = globalThis.localStorage?.getItem(LANGUAGE_STORAGE_KEY);
  return supportedLanguages.includes(stored) ? stored : DEFAULT_LANGUAGE;
}

function formatMessage(template, params) {
  if (!params) {
    return template;
  }

  return Object.entries(params).reduce(
    (message, [key, value]) => message.replaceAll(`{${key}}`, value ?? ""),
    template,
  );
}

function humanizeCode(code) {
  return String(code ?? "")
    .toLowerCase()
    .split("_")
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

export function I18nProvider({ children }) {
  const [language, setLanguageState] = useState(loadLanguage);

  const value = useMemo(() => {
    function setLanguage(nextLanguage) {
      if (!supportedLanguages.includes(nextLanguage)) {
        return;
      }

      setLanguageState(nextLanguage);
      globalThis.localStorage?.setItem(LANGUAGE_STORAGE_KEY, nextLanguage);
    }

    function t(key, params) {
      const template = messages[language]?.[key] ?? messages.en[key] ?? key;
      return formatMessage(template, params);
    }

    function codeLabel(group, code) {
      if (!code) {
        return "";
      }

      return codeMessages[language]?.[group]?.[code] ?? codeMessages.en[group]?.[code] ?? humanizeCode(code);
    }

    return {
      language,
      setLanguage,
      supportedLanguages,
      t,
      codeLabel,
    };
  }, [language]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error("useI18n must be used within I18nProvider");
  }

  return context;
}
