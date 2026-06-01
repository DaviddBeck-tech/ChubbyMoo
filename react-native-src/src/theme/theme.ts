export const PALETTE = {
  backgroundCream: '#FCFAF5',  // Warm cozy off-white
  backgroundCard: '#FFFFFF',   // Base white card layers
  
  primaryLavender: '#E0BBE4',  // Primary lavender violet for icons / major buttons
  secondaryPink: '#FFD1DC',    // Tender rose pink container
  focusPink: '#FFB7C5',        // High-contrast pink state
  
  // Custom status color palettes mapped from html designs
  successMint: '#B5EAD7',       // Completed task background 
  successMintBorder: '#A0D9C5', // Green status border
  successMintText: '#3B7A57',   // Clean font readable color
  
  warningPastel: '#FFFFD1',     // Preformed moved tasks
  warningPastelBorder: '#F0F0C0',
  warningPastelText: '#808030',
  
  errorCoral: '#FFB3B3',        // Delicates delete and high warnings
  errorCoralBorder: '#FFA0A0',

  textCharcoal: '#333333',     // Elegant soft text charcoal
  textGray: '#7A7A7A',         // Secondary body text
  textLightGray: '#A0A0A0',    // Muted timestamps / non active indicators
};

export const SPACING = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
};

export const ROUNDED = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24, // Outstanding 24px/dp corners for lovely card aesthetics
  full: 9999,
};

export const SHADOWS = {
  sm: {
    shadowColor: '#333333',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  md: {
    shadowColor: '#333333',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.12,
    shadowRadius: 8,
    elevation: 4,
  },
  lg: {
    shadowColor: '#E0BBE4',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.2,
    shadowRadius: 16,
    elevation: 8,
  },
};

export const TYPOGRAPHY = {
  displayLarge: {
    fontSize: 28,
    fontWeight: 'bold' as const,
    color: PALETTE.textCharcoal,
  },
  titleLarge: {
    fontSize: 22,
    fontWeight: 'bold' as const,
    color: PALETTE.textCharcoal,
  },
  titleMedium: {
    fontSize: 18,
    fontWeight: '600' as const,
    color: PALETTE.textCharcoal,
  },
  bodyLarge: {
    fontSize: 16,
    fontWeight: 'normal' as const,
    color: PALETTE.textCharcoal,
  },
  bodyMedium: {
    fontSize: 14,
    fontWeight: 'normal' as const,
    color: PALETTE.textGray,
  },
  bodySmall: {
    fontSize: 12,
    fontWeight: '500' as const,
    color: PALETTE.textGray,
  },
  labelBold: {
    fontSize: 12,
    fontWeight: 'bold' as const,
    color: PALETTE.textCharcoal,
  },
};
