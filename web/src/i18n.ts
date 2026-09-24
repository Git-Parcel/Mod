import { createI18n } from "vue-i18n";
import enUS from "./locales/en-US.json";
import zhCN from "./locales/zh-CN.json";

export type Locale = "zh-CN" | "en-US";
export const LOCALES: Locale[] = ["zh-CN", "en-US"];
const LOCALE_STORAGE_KEY = "gitparcel-locale";

function detectLocale(): Locale {
  // Guarded so the module can load outside a browser (unit tests).
  const stored = typeof localStorage === "undefined"
    ? null
    : localStorage.getItem(LOCALE_STORAGE_KEY);
  if (stored === "zh-CN" || stored === "en-US") {
    return stored;
  }
  return typeof navigator === "undefined" ||
      !navigator.language.toLowerCase().startsWith("zh")
    ? "en-US"
    : "zh-CN";
}

export const i18n = createI18n({
  legacy: false,
  locale: detectLocale(),
  fallbackLocale: "en-US",
  messages: { "zh-CN": zhCN, "en-US": enUS },
});

export function setLocale(locale: Locale): void {
  i18n.global.locale.value = locale;
  localStorage.setItem(LOCALE_STORAGE_KEY, locale);
}

/** True when the current locale has a translation for the key. */
export function hasTranslation(key: string): boolean {
  return i18n.global.te(key);
}

/**
 * Resolves a backend-provided identifier (operation kind, dimension id, source…) to a
 * localized label, falling back to the raw value when no translation exists.
 */
export function translateId(prefix: string, raw: string): string {
  const key = `${prefix}.${raw.replace(/[^A-Za-z0-9_]/g, "_")}`;
  return hasTranslation(key) ? i18n.global.t(key) : raw;
}

export function currentLocale(): Locale {
  return i18n.global.locale.value as Locale;
}
