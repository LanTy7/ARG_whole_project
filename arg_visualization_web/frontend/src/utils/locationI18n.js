/**
 * 根据当前语言格式化后端返回的中文地点（如登录位置）。
 * 后端/数据库存的是中文（ip-api.com lang=zh-CN），英文界面下通过 Wikidata API 查英文名，无内置大地名表。
 * @see https://www.wikidata.org/w/api.php (wbsearchentities 返回多语言 label)
 */

const WIKIDATA_SEARCH =
  'https://www.wikidata.org/w/api.php?action=wbsearchentities&format=json&origin=*&language=zh&limit=5';

// 内存缓存：中文地名 -> 英文（或原样），避免重复请求
const cache = new Map();

// 进行中的请求：同一字符串只发一次请求
const pending = new Map();

/**
 * 用 Wikidata API 根据中文地名查英文 label（单条）。
 * @param {string} zhText - 中文地名
 * @returns {Promise<string>} 解析为英文名，失败则返回原字符串
 */
function fetchEnglishLabel(zhText) {
  const key = zhText.trim();
  if (!key) return Promise.resolve('');

  const cached = cache.get(key);
  if (cached !== undefined) return Promise.resolve(cached);

  if (pending.has(key)) return pending.get(key);

  const p = fetch(
    `${WIKIDATA_SEARCH}&search=${encodeURIComponent(key)}`
  )
    .then((res) => res.json())
    .then((data) => {
      const list = data?.search;
      if (!Array.isArray(list) || list.length === 0) {
        cache.set(key, key);
        return key;
      }
      // 优先取第一条带英文 label 的结果（地名类）
      for (const item of list) {
        const label = item?.display?.label ?? item?.label;
        if (typeof label === 'string' && /^[\x00-\x7F\s]+$/.test(label)) {
          cache.set(key, label);
          return label;
        }
        if (label?.value && label?.language === 'en') {
          cache.set(key, label.value);
          return label.value;
        }
      }
      cache.set(key, key);
      return key;
    })
    .catch(() => {
      cache.set(key, key);
      return key;
    })
    .finally(() => {
      pending.delete(key);
    });

  pending.set(key, p);
  return p;
}

/**
 * 仅用于切分「省+市」无空格字符串的中国省份/直辖市前缀（不包含翻译）。
 * 按长度降序，便于最长匹配。
 */
const CN_PROVINCE_PREFIXES = [
  '新疆维吾尔自治区', '广西壮族自治区', '宁夏回族自治区', '西藏自治区', '内蒙古自治区',
  '黑龙江省', '吉林省', '辽宁省', '河北省', '河南省', '山东省', '山西省', '陕西省',
  '江苏省', '浙江省', '安徽省', '福建省', '江西省', '湖北省', '湖南省', '广东省',
  '海南省', '四川省', '贵州省', '云南省', '青海省', '甘肃省', '台湾省',
  '北京市', '上海市', '天津市', '重庆市', '香港', '澳门',
  '新疆', '广西', '宁夏', '西藏', '内蒙古', '黑龙江', '吉林', '辽宁',
  '河北', '河南', '山东', '山西', '陕西', '江苏', '浙江', '安徽', '福建',
  '江西', '湖北', '湖南', '广东', '海南', '四川', '贵州', '云南', '青海', '甘肃', '台湾',
  '北京', '上海', '天津', '重庆',
].sort((a, b) => b.length - a.length);

/**
 * 将「省+市」无空格字符串拆成 [省, 市] 或 [整串]。
 */
function splitChineseLocation(s) {
  for (const prefix of CN_PROVINCE_PREFIXES) {
    if (s.startsWith(prefix) && s.length > prefix.length) {
      return [prefix, s.slice(prefix.length)];
    }
  }
  return [s];
}

/** 常见无需请求 API 的固定值 */
const FIXED_EN = { 未知: 'Unknown' };

/**
 * 异步：把后端返回的中文地点转成英文（Wikidata），带缓存。
 * @param {string} locationStr - 后端返回的中文地点
 * @returns {Promise<string>} 英文地名或原串
 */
export async function formatLocationAsync(locationStr) {
  if (!locationStr || typeof locationStr !== 'string') return '';
  const trimmed = locationStr.trim();
  if (!trimmed) return '';
  if (FIXED_EN[trimmed]) return FIXED_EN[trimmed];

  // 1. 含空格（如 "美国 加利福尼亚 洛杉矶"）：按空格拆段，逐段查英文，再按「城市, 州, 国家」顺序拼接
  if (trimmed.includes(' ')) {
    const parts = trimmed.split(/\s+/).filter(Boolean);
    const enParts = await Promise.all(parts.map((p) => fetchEnglishLabel(p)));
    return enParts.reverse().join(', ');
  }

  // 2. 先整串查一次，若得到英文就直接用
  const fullEn = await fetchEnglishLabel(trimmed);
  if (fullEn !== trimmed) return fullEn;

  // 3. 无空格且整串无结果（如 "浙江嘉兴"）：按省名拆成 [省, 市]，再分别查
  const segments = splitChineseLocation(trimmed);
  if (segments.length <= 1) return trimmed;
  const enSegments = await Promise.all(segments.map((seg) => fetchEnglishLabel(seg)));
  return enSegments.reverse().join(', ');
}

/**
 * 同步：仅按语言返回是否用原文。英文时返回原串作为占位，实际展示需用 formatLocationAsync 的结果。
 * @param {string} locationStr - 后端返回的中文地点
 * @param {string} locale - 'zh' | 'en'
 * @returns {string} 中文时直接返回原串；英文时返回原串（占位，配合异步结果替换）
 */
export function formatLocation(locationStr, locale) {
  if (!locationStr || typeof locationStr !== 'string') return '';
  const trimmed = locationStr.trim();
  if (!trimmed) return '';
  if (locale === 'zh') return trimmed;
  // 英文：若已有缓存则直接返回，否则先返回原串，由调用方用 formatLocationAsync 更新
  return cache.get(trimmed) ?? trimmed;
}

export { cache as locationCache };
