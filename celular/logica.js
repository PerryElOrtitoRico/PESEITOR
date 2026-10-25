// Lógica pura de la app del celular de RUTINEITOR: notación, plan, registro y nombres.
// No toca la pantalla ni localStorage; ninguna función modifica sus argumentos.
"use strict";

const Logica = {
  /** Misma gramática y reglas que la sección 3 (igual que NotacionRegistro de Java).
   *  planificadas: [{reps, kg}]. Devuelve [{reps, kg}]. Lanza Error con mensaje en español si es inválido. */
  parseNotacion(texto, planificadas) {
    if (typeof texto !== "string") throw new Error("casilla vacía");
    const t = texto.replace(/\s+/g, "");
    if (t === "") throw new Error("casilla vacía");
    if (t === "0") return [];
    const plan = Array.isArray(planificadas) ? planificadas : [];
    const partes = t.split(".");
    const res = [];
    for (let i = 0; i < partes.length; i++) {
      const p = partes[i];
      if (p === "") throw new Error("falta una serie entre puntos en \"" + texto + "\"");
      // reps 1..99 sin ceros a la izquierda; kg opcional con decimales con coma
      const m = /^([1-9][0-9]?)(?:-([0-9]+(?:,[0-9]+)?))?$/.exec(p);
      if (!m) throw new Error("serie inválida: \"" + p + "\" (reps de 1 a 99, kg con coma)");
      const reps = Number(m[1]);
      let kg;
      if (m[2] !== undefined) {
        kg = Number(m[2].replace(",", "."));
        if (!(kg > 0)) throw new Error("el peso tiene que ser mayor que 0: \"" + p + "\"");
      } else {
        if (plan.length === 0) throw new Error("indicá el peso de la serie \"" + p + "\" (la fila no tiene series planificadas)");
        kg = plan[Math.min(i, plan.length - 1)].kg;
      }
      res.push({ reps, kg });
    }
    return res;
  },

  /** true/false, sin lanzar. */
  esNotacionValida(texto, planificadas) {
    try {
      Logica.parseNotacion(texto, planificadas);
      return true;
    } catch (e) {
      return false;
    }
  },

  /** Valida un plan ya parseado (objeto): formato "rutineitor-plan", version 1, sesiones es un array.
   *  Devuelve el mismo objeto o lanza Error. */
  validarPlan(plan) {
    if (plan === null || typeof plan !== "object") throw new Error("El archivo no es un plan de RUTINEITOR");
    if (plan.formato !== "rutineitor-plan") {
      throw new Error("No es un plan de RUTINEITOR (formato \"" + plan.formato + "\")");
    }
    if (plan.version !== 1) throw new Error("Versión del plan no soportada: " + plan.version);
    if (!Array.isArray(plan.sesiones)) throw new Error("El plan no tiene la lista de sesiones");
    return plan;
  },

  /** La sesión del plan con ese microciclo y día, o null. */
  sesionDelPlan(plan, microciclo, dia) {
    if (!plan || !Array.isArray(plan.sesiones)) return null;
    return plan.sesiones.find(s => s.microciclo === microciclo && s.dia === dia) || null;
  },

  /** Registro vacío para ese plan: { formato: "rutineitor-registro", version: 1, perfil, mesocicloId, sesiones: [] } */
  registroVacio(plan) {
    return { formato: "rutineitor-registro", version: 1, perfil: plan.perfil, mesocicloId: plan.mesocicloId, sesiones: [] };
  },

  /** La sesión del registro con ese microciclo y día, o null. */
  sesionDelRegistro(registro, microciclo, dia) {
    if (!registro || !Array.isArray(registro.sesiones)) return null;
    return registro.sesiones.find(s => s.microciclo === microciclo && s.dia === dia) || null;
  },

  /** Devuelve un registro NUEVO con la sesión agregada o reemplazada (misma clave microciclo+día),
   *  las sesiones ordenadas por (microciclo, dia) y la sesión pasada por limpiarSesion. */
  guardarSesion(registro, sesion) {
    const limpia = Logica.limpiarSesion(sesion);
    const otras = (registro.sesiones || [])
      .filter(s => !(s.microciclo === limpia.microciclo && s.dia === limpia.dia));
    const sesiones = otras.concat([limpia])
      .sort((a, b) => a.microciclo - b.microciclo || a.dia - b.dia);
    return Object.assign({}, registro, { sesiones });
  },

  /** Copia de la sesión sin ruido: saca de cada ejercicio las filas con texto vacío o solo espacios (y recorta
   *  los espacios del resto), y saca los ejercicios que quedan sin filas y con esfuerzo null. */
  limpiarSesion(sesion) {
    const ejercicios = [];
    for (const ej of sesion.ejercicios || []) {
      const filas = {};
      for (const [etiqueta, texto] of Object.entries(ej.filas || {})) {
        const t = typeof texto === "string" ? texto.trim() : "";
        if (t !== "") filas[etiqueta] = t;
      }
      const esfuerzo = ej.esfuerzo === undefined ? null : ej.esfuerzo;
      if (Object.keys(filas).length === 0 && esfuerzo === null) continue;
      ejercicios.push({ bloque: ej.bloque, esfuerzo, filas });
    }
    return Object.assign({}, sesion, { ejercicios });
  },

  /** Pista gris para la casilla: notación de las reps y los kg distintos en orden, con coma decimal.
   *  [{3,120},{3,120},{3,127.5}] → "3.3.3 · 120/127,5"; [{12,0},{12,0}] → "12.12"; [] → "". */
  pista(series) {
    if (!Array.isArray(series) || series.length === 0) return "";
    const reps = series.map(s => s.reps).join(".");
    const kgs = [];
    for (const s of series) {
      if (s.kg > 0 && !kgs.includes(s.kg)) kgs.push(s.kg);
    }
    if (kgs.length === 0) return reps;
    return reps + " · " + kgs.map(k => String(k).replace(".", ",")).join("/");
  },

  /** "registro-<perfil>-<inicio>.json", con los espacios del perfil cambiados por "_". */
  nombreArchivoRegistro(plan) {
    return "registro-" + String(plan.perfil).replace(/\s/g, "_") + "-" + plan.inicio + ".json";
  },
};
