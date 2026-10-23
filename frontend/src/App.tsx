import { useCallback, useEffect, useState, type FormEvent } from 'react';

type Tab = 'shorten' | 'links';
type Link = { id: number; originalUrl: string; shortenedUrl: string };
type LinkListResponse = {
  links: Link[];
  fromCache: boolean;
  countdownEnabled: boolean;
  ttlMillis: number;
};
type CacheStatus = { active: boolean; remainingMillis: number };

function initialTab(): Tab {
  try { return localStorage.getItem('shortener-tab') === 'links' ? 'links' : 'shorten'; }
  catch { return 'shorten'; }
}

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Se produjo un error inesperado';
}

async function parseApiError(response: Response): Promise<Error> {
  try {
    const payload = await response.json() as { message?: string };
    return new Error(payload.message || `Error HTTP ${response.status}`);
  } catch { return new Error(`Error HTTP ${response.status}`); }
}

export default function App() {
  const [tab, setTab] = useState<Tab>(initialTab);
  const [url, setUrl] = useState('');
  const [creating, setCreating] = useState(false);
  const [created, setCreated] = useState<Link | null>(null);
  const [links, setLinks] = useState<Link[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [origin, setOrigin] = useState<'mongo' | 'redis' | null>(null);
  const [cacheStatus, setCacheStatus] = useState<CacheStatus>({ active: false, remainingMillis: 0 });
  const [socketConnected, setSocketConnected] = useState(false);
  const [copied, setCopied] = useState('');
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const loadLinks = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetch('/api/links', { cache: 'no-store' });
      if (!response.ok) throw await parseApiError(response);
      const result = await response.json() as LinkListResponse;
      setLinks(result.links);
      const cacheIsActive = result.fromCache && result.countdownEnabled;
      setOrigin(cacheIsActive ? 'redis' : 'mongo');
      // WebSocket enviará el estado exacto del servidor cada segundo.
      // Esta respuesta permite mostrar el indicador sin esperar al siguiente tick.
      setCacheStatus({ active: result.countdownEnabled, remainingMillis: result.ttlMillis });
    } catch (err) {
      setError(getErrorMessage(err));
    } finally { setLoading(false); }
  }, []);

  useEffect(() => {
    try { localStorage.setItem('shortener-tab', tab); } catch { /* navegador restringido */ }
    if (tab === 'links') void loadLinks();
  }, [tab, loadLinks]);

  useEffect(() => {
    setOrigin(cacheStatus.active ? 'redis' : 'mongo');
  }, [cacheStatus.active]);

  useEffect(() => {
    // WebSocket persistente: el servidor lee PTTL de Redis incluso tras recargar.
    // Reconexión simple cuando se pierde el socket temporalmente.
    let disposed = false;
    let socket: WebSocket | null = null;
    let reconnectTimer: ReturnType<typeof setTimeout> | undefined;
    const connect = () => {
      if (disposed) return;
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      socket = new WebSocket(`${protocol}//${window.location.host}/ws/cache`);
      socket.onopen = () => setSocketConnected(true);
      socket.onmessage = (event: MessageEvent<string>) => {
        try { setCacheStatus(JSON.parse(event.data) as CacheStatus); } catch { /* mensaje desconocido */ }
      };
      socket.onclose = () => {
        setSocketConnected(false);
        if (!disposed) reconnectTimer = setTimeout(connect, 2000);
      };
      socket.onerror = () => { socket?.close(); };
    };
    connect();
    return () => {
      disposed = true;
      if (reconnectTimer) clearTimeout(reconnectTimer);
      socket?.close();
    };
  }, []);

  const showLinks = () => {
    if (tab === 'links') void loadLinks(); // Pulsar otra vez = consultar de nuevo sin renovar el TTL.
    else setTab('links');
  };

  const showShorten = () => { setTab('shorten'); setError(''); };

  const shorten = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setCreating(true);
    setError('');
    setCreated(null);
    try {
      const response = await fetch('/api/links', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: url.trim() }),
      });
      if (!response.ok) throw await parseApiError(response);
      const result = await response.json() as Link;
      setCreated(result);
      setCacheStatus({ active: false, remainingMillis: 0 });
    } catch (err) { setError(getErrorMessage(err)); }
    finally { setCreating(false); }
  };

  const copyLink = async (value: string) => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(value);
    } catch { setError('No se pudo copiar. Puedes seleccionar el enlace manualmente.'); }
  };

  const deleteLink = async (id: number) => {
    setDeletingId(id);
    setError('');
    try {
      const response = await fetch(`/api/links/${id}`, { method: 'DELETE' });
      if (!response.ok) throw await parseApiError(response);
      await loadLinks();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setDeletingId(null);
    }
  };

  const remainingSeconds = Math.ceil(cacheStatus.remainingMillis / 1000);

  return (
    <main className="app-shell">
      <header className="header">
        <p className="eyebrow"> PARCIAL 🤔</p>
        <h1>Acortador de enlaces 🔗</h1>
      </header>

      <section className="workspace" aria-label="Acortador de enlaces">
        <div className="toolbar">
          <div className="tabs" role="tablist" aria-label="Secciones">
            <button type="button" role="tab" aria-selected={tab === 'shorten'}
              className={`tab ${tab === 'shorten' ? 'active' : ''}`} onClick={showShorten}>
              Acortar enlaces
            </button>
            <button type="button" role="tab" aria-selected={tab === 'links'}
              className={`tab ${tab === 'links' ? 'active' : ''}`} onClick={showLinks}>
              Ver enlaces
            </button>
          </div>
          {tab === 'links' && (
            <div className="cache-metrics" aria-live="polite">
              {cacheStatus.active && remainingSeconds > 0 ? (
                <span className="cache-timer">Caché Redis: <strong>{remainingSeconds} segundos</strong></span>
              ) : <span className="cache-placeholder">Caché sin contador activo</span>}
              <span className={`connection-dot ${socketConnected ? 'online' : 'offline'}`} title={socketConnected ? 'WebSocket conectado' : 'WebSocket desconectado'} />
            </div>
          )}
        </div>

        {error && <div className="alert" role="alert">{error}</div>}

        {tab === 'shorten' ? (
          <div className="shorten-pane" role="tabpanel">
            <div className="section-title">
              <h2>Acortar un enlace</h2>
              <p>Pega una URL y obtén un enlace corto y redirigible </p>
            </div>
            <form className="url-form" onSubmit={shorten}>
              <label htmlFor="original-url">Enlace original</label>
              <input id="original-url" type="url" required value={url}
                onChange={e => setUrl(e.target.value)} placeholder="https://www.youtube.com/watch?v=..."
                autoFocus autoComplete="url" />
              <small className="input-hint">Los enlaces acortados tienen un mínimo de 8 caracteres.</small>
              <button type="submit" className="primary-button" disabled={creating}>
                {creating ? 'Acortando...' : 'Acortar'}
              </button>
            </form>
            {created && <div className="result" aria-live="polite">
              <p className="result-label">Enlace acortado correctamente</p>
              <div className="result-row">
                <a href={created.shortenedUrl} target="_blank" rel="noreferrer">{created.shortenedUrl}</a>
                <button type="button" className="secondary-button" onClick={() => void copyLink(created.shortenedUrl)}>
                  {copied === created.shortenedUrl ? 'Copiado' : 'Copiar'}
                </button>
              </div>
            </div>}
          </div>
        ) : (
          <div className="links-pane" role="tabpanel">
            <div className="section-title table-heading">
              <div><h2>Enlaces registrados</h2><p>Enlaces originales: Guardados de forma permanente en MongoDB.</p></div>
              {origin && <span className="source-indicator">Origen: {origin === 'redis' ? 'Redis (caché)' : 'MongoDB'}</span>}
            </div>
            <div className="table-scroll">
              <table>
                <thead><tr><th scope="col">ID</th><th scope="col">Enlace original</th><th scope="col">Enlace acortado</th></tr></thead>
                <tbody>
                  {links.map(link => (
                    <tr key={link.id}>
                      <td className="id-cell">{link.id}</td>
                      <td><a href={link.originalUrl} target="_blank" rel="noreferrer">{link.originalUrl}</a></td>
                      <td>
                        <div className="link-cell">
                          <a href={link.shortenedUrl} target="_blank" rel="noreferrer">{link.shortenedUrl}</a>
                          <button type="button" className="danger-button" onClick={() => void deleteLink(link.id)} disabled={deletingId === link.id}>
                            {deletingId === link.id ? 'Eliminando...' : 'Eliminar'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {!loading && links.length === 0 && (
                    <tr><td colSpan={3} className="empty-cell">No hay enlaces. Acorta un enlace para registrar el primero.</td></tr>
                  )}
                  {loading && <tr><td colSpan={3} className="empty-cell">Consultando enlaces...</td></tr>}
                </tbody>
              </table>
            </div>
            <p className="table-note">Cada enlace acortado tiene un mínimo de 8 caracteres y se actualiza automáticamente al consultar el caché.</p>
          </div>
        )}
      </section>
    </main>
  );
}
